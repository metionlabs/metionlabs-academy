package com.metionlabs.academy.jpa.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 책 엔티티. 한 명의 저자가 여러 권을 쓸 수 있다. (N:1 의 N 쪽)
 *
 * <p>DB 구조상 <b>FK 를 가진 쪽이 이 클래스</b>다.
 * {@code books} 테이블에 {@code author_id} 컬럼이 있고, 이 컬럼이 {@code authors.id} 를 가리킨다.
 *
 * <pre>
 *   authors                books
 *   -------                -----------------
 *   id   (PK)  &lt;-------->  author_id  (FK)
 *   name                   id         (PK)
 *   email                  title
 *                          price
 *                          published_date
 * </pre>
 */
@Entity
@Table(name = "books")
public class Book {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String title;

	/**
	 * 가격.
	 *
	 * <p><b>돈을 다룰 때 {@code double} 을 쓰면 안 된다.</b>
	 * {@code double} 은 2진 부동소수점이라 0.1 같은 값을 정확히 표현하지 못한다.
	 * 계산을 반복하면 오차가 쌓여서 1원씩 안 맞는 사고가 난다.
	 *
	 * <p>여기서는 원 단위 정수라 {@code Integer} 로 충분하다.
	 * 소수점이 필요한 금액(환율, 세금 계산 등)이라면 {@code BigDecimal} 을 쓴다.
	 */
	@Column(nullable = false)
	private Integer price;

	/**
	 * 출간일.
	 *
	 * <p>{@code java.util.Date} 가 아니라 {@code java.time.LocalDate} 를 쓴다.
	 * 옛 Date 클래스는 값이 변경 가능하고(mutable) 시간대 처리가 혼란스러워서
	 * 자바 8 이후로는 사실상 쓰지 않는다.
	 *
	 * <p>{@code LocalDate} 는 날짜만(시간 없음) 다룬다.
	 * 시간까지 필요하면 {@code LocalDateTime}, 시간대까지면 {@code ZonedDateTime} 을 쓴다.
	 *
	 * <p>{@code @Column(name = ...)} 으로 컬럼명을 지정했다.
	 * 지정하지 않아도 스프링이 카멜케이스를 스네이크케이스로 바꿔주지만
	 * ({@code publishedDate} -> {@code published_date}), 명시하면 오해의 여지가 없다.
	 */
	@Column(name = "published_date", nullable = false)
	private LocalDate publishedDate;

	/**
	 * 저자. (N:1 관계의 주인)
	 *
	 * <p><b>이 필드가 "연관관계의 주인"</b>이다.
	 * {@code Author.books} 에 붙은 {@code mappedBy = "author"} 의 "author" 가 바로 이 필드를 가리킨다.
	 * 주인이라는 건 <b>이 필드의 값이 실제 FK 컬럼에 반영된다</b>는 뜻이다.
	 *
	 * <p><b>{@code fetch = FetchType.LAZY} 를 명시한 이유 — 중요하다.</b>
	 * {@code @ManyToOne} 의 기본값은 {@code EAGER}(즉시 로딩)다.
	 * 즉 아무 설정도 안 하면, 책 하나를 조회할 때마다 저자까지 항상 같이 SELECT 한다.
	 * 저자 정보가 필요 없는 상황에서도 무조건 쿼리가 나가고,
	 * 책 100권을 조회하면 저자 조회 쿼리가 줄줄이 따라붙는다.
	 *
	 * <p>그래서 <b>{@code @ManyToOne} 은 항상 LAZY 로 바꿔 쓰는 것이 정석</b>이다.
	 * 필요할 때만 fetch join 으로 한 번에 가져오는 편이 훨씬 낫다.
	 * ({@code AuthorRepository.findAllWithBooks()} 참고)
	 *
	 * <p>{@code @JoinColumn(name = "author_id")} = 이 관계가 사용할 FK 컬럼 이름.
	 * {@code nullable = false} 이므로 저자 없는 책은 저장할 수 없다.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false)
	private Author author;

	/**
	 * JPA 용 기본 생성자. (자세한 설명은 {@link Author#Author()} 참고)
	 */
	protected Book() {
	}

	/**
	 * @param title         제목
	 * @param price         가격(원)
	 * @param publishedDate 출간일
	 */
	public Book(String title, Integer price, LocalDate publishedDate) {
		this.title = title;
		this.price = price;
		this.publishedDate = publishedDate;
	}

	/**
	 * 저자를 설정한다.
	 *
	 * <p>이 메서드는 {@link Author#addBook(Book)} 안에서 호출된다.
	 * 직접 호출하기보다 {@code author.addBook(book)} 을 쓰는 편이 안전하다.
	 * 그래야 양쪽 방향이 모두 맞춰지기 때문이다.
	 *
	 * <p>public 인 이유는 같은 패키지 밖의 서비스 코드에서도 필요할 수 있어서지만,
	 * 실무에서는 {@code package-private} 로 좁혀 접근을 제한하기도 한다.
	 *
	 * @param author 저자
	 */
	public void setAuthor(Author author) {
		this.author = author;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public Integer getPrice() {
		return price;
	}

	public LocalDate getPublishedDate() {
		return publishedDate;
	}

	/**
	 * 저자를 반환한다.
	 *
	 * <p><b>주의:</b> LAZY 로딩이라 이 메서드를 호출하는 순간 DB 조회가 발생할 수 있다.
	 * 그리고 트랜잭션이 이미 끝난 뒤에 호출하면
	 * {@code LazyInitializationException} 이 터진다.
	 * (JPA 가 DB 와 연결된 상태에서만 지연 로딩을 할 수 있기 때문)
	 *
	 * <p>그래서 컨트롤러에서 엔티티를 그대로 반환하지 않고,
	 * 트랜잭션 안에서 DTO 로 변환해 내보내는 것이다.
	 *
	 * @return 저자
	 */
	public Author getAuthor() {
		return author;
	}

}
