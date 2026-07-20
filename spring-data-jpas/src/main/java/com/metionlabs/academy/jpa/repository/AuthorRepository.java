package com.metionlabs.academy.jpa.repository;

import java.util.List;
import java.util.Optional;

import com.metionlabs.academy.jpa.domain.Author;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 저자 저장소.
 *
 * <p><b>구현 클래스가 없는데 어떻게 동작하는가?</b>
 * 이건 인터페이스일 뿐인데 우리는 구현체를 만든 적이 없다.
 * 애플리케이션이 시작될 때 <b>Spring Data JPA 가 이 인터페이스의 구현체를 자동으로 만들어</b>
 * 스프링 빈으로 등록한다. 우리는 그걸 주입받아 쓰기만 하면 된다.
 *
 * <p>{@code JpaRepository<Author, Long>} 의 두 타입 파라미터:
 * <ul>
 *   <li>{@code Author} — 이 저장소가 다룰 엔티티</li>
 *   <li>{@code Long} — 그 엔티티의 기본키 타입</li>
 * </ul>
 *
 * <p>상속만으로 이런 메서드들이 공짜로 생긴다:
 * <pre>
 *   save(entity)      저장 또는 수정
 *   findById(id)      단건 조회 (Optional 로 반환)
 *   findAll()         전체 조회
 *   delete(entity)    삭제
 *   count()           개수
 * </pre>
 */
public interface AuthorRepository extends JpaRepository<Author, Long> {

	/**
	 * 이메일로 저자를 찾는다.
	 *
	 * <p><b>쿼리 메서드(derived query)</b> — 메서드 <b>이름</b>만으로 쿼리가 만들어진다.
	 * {@code findByEmail} 이라는 이름을 Spring Data 가 해석해서
	 * {@code SELECT * FROM authors WHERE email = ?} 를 생성한다.
	 *
	 * <p>이름 규칙 예시:
	 * <pre>
	 *   findByName(String name)                    WHERE name = ?
	 *   findByNameContaining(String keyword)       WHERE name LIKE %?%
	 *   findByPriceGreaterThan(int price)          WHERE price &gt; ?
	 *   findByNameOrderByIdDesc(String name)       WHERE name = ? ORDER BY id DESC
	 *   existsByEmail(String email)                존재 여부만 boolean 으로
	 *   countByName(String name)                   개수만
	 * </pre>
	 *
	 * <p><b>주의:</b> 이름에 오타가 있거나 없는 필드를 참조하면
	 * <b>애플리케이션 시작 시점에</b> 에러가 난다. 런타임에 조용히 실패하지 않는다는 점에서
	 * 오히려 안전한 동작이다.
	 *
	 * <p><b>왜 {@code Optional} 인가?</b>
	 * 해당 이메일의 저자가 없을 수 있기 때문이다.
	 * {@code null} 을 반환하면 호출하는 쪽에서 null 체크를 깜빡해 NPE 가 나기 쉽다.
	 * {@code Optional} 은 "없을 수도 있다" 를 타입으로 강제해서 처리하게 만든다.
	 *
	 * @param email 이메일
	 * @return 저자 (없으면 빈 Optional)
	 */
	Optional<Author> findByEmail(String email);

	/**
	 * 이름에 특정 문자열이 포함된 저자들을 찾는다. ({@code LIKE %keyword%})
	 *
	 * @param keyword 검색어
	 * @return 조건에 맞는 저자 목록 (없으면 빈 리스트. null 이 아니다)
	 */
	List<Author> findByNameContaining(String keyword);

	/**
	 * 모든 저자를 <b>책 목록까지 한 번의 쿼리로</b> 가져온다. (N+1 문제 해결책)
	 *
	 * <p>=========================================================================
	 * <p><b>N+1 문제란?</b>
	 * <p>=========================================================================
	 *
	 * <p>{@code findAll()} 로 저자 10명을 조회하고, 각 저자의 책 목록을 꺼내 쓴다고 하자.
	 * {@code Author.books} 는 LAZY 라서 실제로 접근할 때 쿼리가 나간다.
	 *
	 * <pre>
	 *   SELECT * FROM authors;                       &lt;- 저자 조회 1번
	 *   SELECT * FROM books WHERE author_id = 1;     &lt;- 1번 저자의 책
	 *   SELECT * FROM books WHERE author_id = 2;     &lt;- 2번 저자의 책
	 *   ...
	 *   SELECT * FROM books WHERE author_id = 10;    &lt;- 10번 저자의 책
	 * </pre>
	 *
	 * <p>총 <b>1 + 10 = 11번</b>의 쿼리. 저자가 1,000명이면 1,001번이다.
	 * 이것이 <b>N+1 문제</b> — 처음 1번의 쿼리(1) 때문에 N번의 추가 쿼리가 발생한다.
	 *
	 * <p>개발할 땐 데이터가 적어서 멀쩡해 보이다가, <b>운영에서 데이터가 쌓이면
	 * 갑자기 응답이 수 초로 느려지는</b> 대표적인 성능 사고다.
	 *
	 * <p>=========================================================================
	 * <p><b>해결: fetch join</b>
	 * <p>=========================================================================
	 *
	 * <p>{@code JOIN FETCH} 는 "조인해서 가져온 걸 그냥 버리지 말고,
	 * 연관 객체까지 채워서 한 번에 달라" 는 뜻이다. 쿼리 1번으로 끝난다.
	 *
	 * <p><b>{@code DISTINCT} 가 필요한 이유:</b>
	 * 저자 1명이 책 3권을 썼다면 조인 결과는 3줄이 되고,
	 * JPA 는 같은 저자 객체를 3번 돌려주게 된다. {@code DISTINCT} 로 중복을 제거한다.
	 *
	 * <p><b>주의 — 컬렉션 fetch join 은 한 번에 하나만 가능하다.</b>
	 * 두 개 이상의 컬렉션을 동시에 fetch join 하면
	 * {@code MultipleBagFetchException} 이 발생한다.
	 * (곱집합이 되어 데이터가 폭증하기 때문에 JPA 가 막는다)
	 * 그럴 땐 하나만 fetch join 하고 나머지는 {@code @BatchSize} 로 푸는 것이 일반적이다.
	 *
	 * <p><b>{@code @Query} 안의 문법은 SQL 이 아니라 JPQL 이다.</b>
	 * 테이블명이 아니라 <b>엔티티 클래스명</b>({@code Author}),
	 * 컬럼명이 아니라 <b>필드명</b>({@code books})을 쓴다.
	 * 그래서 DB 를 MySQL 에서 PostgreSQL 로 바꿔도 이 쿼리는 그대로 동작한다.
	 *
	 * @return 책 목록까지 채워진 저자 전체
	 */
	@Query("SELECT DISTINCT a FROM Author a LEFT JOIN FETCH a.books")
	List<Author> findAllWithBooks();
	// LEFT JOIN 인 이유: 그냥 JOIN(INNER)을 쓰면 책이 한 권도 없는 저자가 결과에서 빠진다.
	// "책 없는 저자도 목록에 나와야 한다" 면 LEFT JOIN 이 맞다.

}
