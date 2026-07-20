package com.metionlabs.academy.jpa.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * 저자(著者) 엔티티.
 *
 * <p><b>엔티티란?</b> DB 테이블 한 줄(row)에 대응하는 자바 객체다.
 * 우리가 이 객체의 필드를 바꾸면 JPA 가 알아서 UPDATE 문을 만들어 DB 에 반영한다.
 * SQL 을 직접 쓰지 않고 자바 객체를 다루듯 DB 를 다루는 것 — 이게 JPA 의 목적이다.
 *
 * <p>이 클래스는 {@code authors} 테이블과 이렇게 대응한다:
 * <pre>
 *   authors 테이블         Author 클래스
 *   ------------------    ----------------
 *   id        BIGINT  <-> Long id
 *   name      VARCHAR <-> String name
 *   email     VARCHAR <-> String email
 * </pre>
 */
@Entity // "이 클래스는 DB 테이블과 연결된다" 는 표시. 이게 있어야 JPA 가 관리 대상으로 인식한다.
@Table(name = "authors")
// @Table 을 생략하면 클래스 이름(Author)을 그대로 테이블명으로 쓴다.
// 굳이 명시하는 이유:
//   1) 테이블명은 보통 복수형(authors)으로 짓는 관례가 있다
//   2) user, order 같은 이름은 DB 예약어라서 그대로 쓰면 SQL 문법 오류가 난다.
//      (order -> orders 로 바꿔야 하는 대표적인 경우)
public class Author {

	/**
	 * 기본키(PK).
	 *
	 * <p>{@code @GeneratedValue} = 값을 직접 넣지 않고 DB 가 자동으로 채우게 한다.
	 *
	 * <p>{@code IDENTITY} 전략은 MySQL 의 {@code AUTO_INCREMENT} 를 사용한다는 뜻이다.
	 * DB 마다 자동 증가 방식이 달라서 전략을 골라줘야 한다:
	 * <ul>
	 *   <li>{@code IDENTITY} — MySQL 의 AUTO_INCREMENT. MySQL 이면 이걸 쓴다.</li>
	 *   <li>{@code SEQUENCE} — Oracle, PostgreSQL 의 시퀀스 객체</li>
	 *   <li>{@code AUTO} — JPA 가 DB 를 보고 알아서 고름 (편하지만 예측이 어렵다)</li>
	 * </ul>
	 *
	 * <p><b>왜 {@code long} 이 아니라 {@code Long} 인가?</b>
	 * 아직 저장하지 않은 객체는 id 가 "없는" 상태여야 하는데,
	 * 기본형 {@code long} 은 값이 없을 수 없어서 자동으로 0 이 된다.
	 * 그러면 JPA 가 "id 가 0 인 기존 데이터" 로 오해할 여지가 생긴다.
	 * 래퍼 타입 {@code Long} 을 쓰면 저장 전에는 {@code null} 이라 "아직 저장 안 됨" 이 명확해진다.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 저자 이름.
	 *
	 * <p>{@code nullable = false} 는 DB 에 {@code NOT NULL} 제약을 건다.
	 * {@code length = 50} 은 {@code VARCHAR(50)} 이 된다. (기본값은 255)
	 *
	 * <p><b>주의:</b> 이 제약은 <b>DB 레벨</b>이라, 위반하면 저장 시점에 예외가 터진다.
	 * 사용자 입력을 검증하려면 이것만으로는 부족하고,
	 * 요청을 받는 지점에서 {@code @NotBlank} 같은 검증을 따로 해야 한다.
	 * (이 예제에서는 DTO 인 {@code AuthorRequest} 에서 처리한다)
	 * 둘은 역할이 다르다 — 검증은 "빨리 거절하기", 제약은 "최후의 방어선".
	 */
	@Column(nullable = false, length = 50)
	private String name;

	/**
	 * 이메일. {@code unique = true} 로 중복 저장을 DB 가 막는다.
	 *
	 * <p>애플리케이션에서 "이미 있는지 조회해보고 없으면 저장" 하는 방식만으로는
	 * 동시에 두 요청이 들어오면 둘 다 통과해버릴 수 있다(경쟁 조건).
	 * DB 의 unique 제약이 있어야 그 순간에도 하나만 성공한다.
	 */
	@Column(nullable = false, length = 100, unique = true)
	private String email;

	/**
	 * 이 저자가 쓴 책들. (1:N 관계)
	 *
	 * <p><b>{@code mappedBy = "author"} 의 의미 — 여기가 초심자가 가장 많이 헷갈리는 부분이다.</b>
	 *
	 * <p>DB 에서 1:N 관계는 <b>N 쪽 테이블이 외래키(FK)를 가진다.</b>
	 * 즉 {@code books} 테이블에 {@code author_id} 컬럼이 있고, {@code authors} 에는 책 목록 컬럼이 없다.
	 *
	 * <p>그런데 자바 객체에서는 양쪽에서 서로를 참조할 수 있다.
	 * 그래서 JPA 에게 "실제 FK 를 관리하는 쪽이 어디인지" 알려줘야 한다.
	 * {@code mappedBy = "author"} = <b>"나는 주인이 아니다. Book 클래스의 author 필드가 주인이다."</b>
	 *
	 * <p>이걸 빠뜨리면 JPA 는 양쪽을 별개 관계로 보고
	 * {@code authors_books} 같은 중간 테이블을 멋대로 만들어버린다.
	 *
	 * <p><b>{@code cascade}, {@code orphanRemoval} 설명:</b>
	 * <ul>
	 *   <li>{@code CascadeType.ALL} — 저자를 저장/삭제하면 책들도 같이 저장/삭제된다.
	 *       저자 없는 책은 존재 의미가 없으므로 이 관계에선 적절하다.
	 *       하지만 <b>아무 관계에나 걸면 위험하다.</b> 예를 들어 "회원 - 주문" 에 걸면
	 *       회원 탈퇴 시 주문 이력까지 삭제되어 정산 데이터가 날아간다.</li>
	 *   <li>{@code orphanRemoval = true} — 리스트에서 책을 빼면 DB 에서도 삭제한다.
	 *       "부모와의 연결이 끊긴 자식(고아)은 지운다" 는 뜻이다.</li>
	 * </ul>
	 *
	 * <p><b>기본 로딩 전략:</b> {@code @OneToMany} 는 기본이 <b>LAZY(지연 로딩)</b> 다.
	 * 저자를 조회할 때 책 목록은 아직 안 읽어오고, 실제로 {@code getBooks()} 를 호출하는
	 * 순간에 SELECT 가 나간다. 이 동작이 N+1 문제의 원인이 된다.
	 * (자세한 건 {@code AuthorRepository} 의 주석 참고)
	 */
	@OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Book> books = new ArrayList<>();
	// 필드에서 바로 new ArrayList<>() 로 초기화하는 이유:
	// null 이면 addBook() 을 호출할 때마다 null 체크를 해야 하고, 깜빡하면 NPE 가 난다.
	// 컬렉션 필드는 "비어 있는 컬렉션" 으로 시작하는 게 안전하다.

	/**
	 * JPA 가 요구하는 기본 생성자.
	 *
	 * <p>JPA 는 DB 에서 읽은 데이터로 객체를 만들 때, 먼저 빈 객체를 생성한 뒤
	 * 리플렉션으로 필드를 채운다. 그래서 파라미터 없는 생성자가 반드시 있어야 한다.
	 *
	 * <p>{@code protected} 로 제한한 이유: 외부에서 {@code new Author()} 로
	 * 이름도 이메일도 없는 불완전한 객체를 만드는 걸 막기 위해서다.
	 * JPA 는 protected 까지는 접근할 수 있어서 동작에 문제가 없다.
	 */
	protected Author() {
	}

	/**
	 * 실제로 사용할 생성자. 필수값을 받아 완전한 객체를 만든다.
	 *
	 * @param name  저자 이름
	 * @param email 이메일 (중복 불가)
	 */
	public Author(String name, String email) {
		this.name = name;
		this.email = email;
	}

	/**
	 * 연관관계 편의 메서드.
	 *
	 * <p><b>왜 이런 메서드가 필요한가?</b>
	 * 양방향 관계에서는 <b>양쪽 객체의 상태를 모두 맞춰줘야</b> 한다.
	 * <pre>
	 *   author.getBooks().add(book);  // 저자 -> 책 방향만 설정됨
	 *   book.setAuthor(author);       // 책 -> 저자 방향도 해줘야 함
	 * </pre>
	 * 둘 중 하나를 빠뜨리면, DB 에는 잘 저장됐는데 <b>메모리 상의 객체만 어긋난 상태</b>가 된다.
	 * 같은 트랜잭션 안에서 조회하면 방금 추가한 책이 안 보이는 기묘한 버그가 생긴다.
	 *
	 * <p>그래서 두 줄을 항상 같이 실행하도록 메서드 하나로 묶는다.
	 * 실수할 여지 자체를 없애는 방식이다.
	 *
	 * @param book 추가할 책
	 */
	public void addBook(Book book) {
		this.books.add(book);
		book.setAuthor(this);
	}

	// ------------------------------------------------------------------------
	// Getter
	// ------------------------------------------------------------------------
	// setter 를 만들지 않은 것에 주목.
	// 아무 데서나 setName() 으로 값을 바꿀 수 있으면, 나중에 "이 값이 어디서 바뀌었지?" 를
	// 추적하기가 매우 어려워진다. 엔티티는 의미 있는 메서드로만 상태를 바꾸게 하는 편이 좋다.
	// (변경이 필요하면 changeEmail(...) 처럼 의도가 드러나는 이름의 메서드를 추가한다)

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public List<Book> getBooks() {
		return books;
	}

}
