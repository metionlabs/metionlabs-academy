package com.metionlabs.academy.jpa.repository;

import java.util.List;

import com.metionlabs.academy.jpa.domain.Book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 책 저장소.
 *
 * <p>{@link AuthorRepository} 가 쿼리 메서드와 fetch join 을 다뤘다면,
 * 여기서는 <b>페이징</b>과 <b>파라미터 바인딩</b>을 다룬다.
 */
public interface BookRepository extends JpaRepository<Book, Long> {

	/**
	 * 특정 가격 이상인 책을 비싼 순으로 조회한다.
	 *
	 * <p>메서드 이름이 길지만 규칙대로 읽으면 그대로 SQL 이 된다:
	 * <pre>
	 *   findBy PriceGreaterThanEqual OrderByPriceDesc
	 *          WHERE price &gt;= ?      ORDER BY price DESC
	 * </pre>
	 *
	 * <p><b>이름이 이보다 더 길어지면 {@code @Query} 로 옮기는 편이 낫다.</b>
	 * 조건이 서너 개만 붙어도 메서드 이름이 100자를 넘어가서 아무도 못 읽는다.
	 * 규칙은 "짧으면 쿼리 메서드, 길거나 복잡하면 @Query".
	 *
	 * @param price 최소 가격
	 * @return 조건에 맞는 책 목록
	 */
	List<Book> findByPriceGreaterThanEqualOrderByPriceDesc(Integer price);

	/**
	 * 제목으로 검색하되 <b>페이지 단위로</b> 가져온다.
	 *
	 * <p><b>왜 페이징이 필요한가?</b>
	 * {@code findAll()} 은 테이블의 모든 행을 메모리로 읽어온다.
	 * 데이터가 100만 건이면 그걸 전부 자바 객체로 만들다가 서버 메모리가 터진다.
	 * 실무에서 목록 조회에 페이징을 안 붙이는 건 시한폭탄을 두는 것과 같다.
	 *
	 * <p>{@code Pageable} 을 파라미터로 받으면 Spring Data 가
	 * {@code LIMIT} / {@code OFFSET} 을 알아서 붙여준다.
	 *
	 * <p>{@code Page} 로 받으면 전체 개수를 세는 {@code COUNT} 쿼리가 추가로 나가서,
	 * "전체 몇 페이지인지" 를 알 수 있다.
	 * 전체 개수가 필요 없다면 {@code Slice} 를 쓰면 COUNT 쿼리를 아낄 수 있다.
	 *
	 * @param keyword  제목 검색어 (부분 일치)
	 * @param pageable 페이지 번호·크기·정렬 정보
	 * @return 검색 결과 한 페이지
	 */
	Page<Book> findByTitleContaining(String keyword, Pageable pageable);

	/**
	 * 특정 저자가 쓴 책들을 조회한다. (JPQL 직접 작성)
	 *
	 * <p><b>{@code :authorId} 는 "이름 있는 파라미터"</b> 다.
	 * {@code @Param("authorId")} 로 어떤 인자가 여기에 들어갈지 연결한다.
	 *
	 * <p><b>[보안] 문자열을 이어붙여서 쿼리를 만들면 절대 안 된다.</b>
	 * <pre>
	 *   // 절대 이렇게 하지 말 것
	 *   "SELECT b FROM Book b WHERE b.author.id = " + authorId
	 * </pre>
	 * 이러면 SQL 인젝션 공격에 뚫린다. 공격자가 입력값에 SQL 조각을 넣어
	 * 데이터를 통째로 빼가거나 삭제할 수 있다.
	 * 파라미터 바인딩({@code :authorId})을 쓰면 입력값이 항상 "값"으로만 취급되어 안전하다.
	 *
	 * <p>참고로 {@code b.author.id} 는 연관 엔티티의 필드를 타고 들어간 것인데,
	 * FK 컬럼({@code author_id})만 보면 되므로 <b>authors 테이블 조인이 발생하지 않는다.</b>
	 *
	 * @param authorId 저자 ID
	 * @return 그 저자의 책 목록
	 */
	@Query("SELECT b FROM Book b WHERE b.author.id = :authorId ORDER BY b.publishedDate DESC")
	List<Book> findByAuthorId(@Param("authorId") Long authorId);

}
