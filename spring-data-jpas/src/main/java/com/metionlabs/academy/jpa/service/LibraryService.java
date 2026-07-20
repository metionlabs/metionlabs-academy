package com.metionlabs.academy.jpa.service;

import java.util.List;

import com.metionlabs.academy.jpa.domain.Author;
import com.metionlabs.academy.jpa.domain.Book;
import com.metionlabs.academy.jpa.repository.AuthorRepository;
import com.metionlabs.academy.jpa.repository.BookRepository;
import com.metionlabs.academy.jpa.web.dto.AuthorRequest;
import com.metionlabs.academy.jpa.web.dto.AuthorResponse;
import com.metionlabs.academy.jpa.web.dto.BookRequest;
import com.metionlabs.academy.jpa.web.dto.BookResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 저자·책 관련 업무 로직.
 *
 * <p><b>왜 컨트롤러에서 바로 리포지토리를 쓰지 않고 서비스를 두는가?</b>
 * <ul>
 *   <li><b>트랜잭션 경계</b> — "여러 DB 작업을 하나로 묶는" 단위가 필요하다.
 *       그 단위는 HTTP 요청(컨트롤러)이 아니라 업무 단위(서비스)여야 한다.</li>
 *   <li><b>재사용</b> — 같은 로직을 웹 요청, 배치 작업, 관리자 API 에서 함께 쓸 수 있다.
 *       컨트롤러에 로직이 있으면 배치에서 재사용할 방법이 없다.</li>
 *   <li><b>테스트</b> — 서비스는 HTTP 없이 단독으로 테스트할 수 있다.</li>
 * </ul>
 *
 * <p>메모리 규칙: <b>컨트롤러는 요청/응답 변환만, 실제 일은 서비스가</b>.
 */
@Service // 스프링이 이 클래스를 빈으로 등록한다.
         // @Component 와 기능은 같지만, 이름으로 역할(업무 로직)이 드러나서 관례적으로 구분해 쓴다.
@Transactional(readOnly = true)
// ----------------------------------------------------------------------------
// 클래스 레벨에 readOnly = true 를 걸어두는 패턴.
//
// 이 클래스의 모든 메서드가 기본적으로 "읽기 전용 트랜잭션" 이 되고,
// 데이터를 바꾸는 메서드에만 @Transactional 을 다시 붙여 덮어쓴다. (아래 create 메서드들)
//
// 왜 이렇게 하나?
//   1) 성능 — 읽기 전용이면 JPA 가 "변경 감지(dirty checking)" 를 위한
//      스냅샷 보관을 생략한다. 조회가 많은 서비스에서 메모리와 CPU 를 아낀다.
//   2) 안전 — 조회 메서드에서 실수로 엔티티 값을 바꿔도 DB 에 반영되지 않는다.
//      "조회했을 뿐인데 데이터가 바뀌는" 사고를 구조적으로 막는다.
// ----------------------------------------------------------------------------
public class LibraryService {

	private final AuthorRepository authorRepository;
	private final BookRepository bookRepository;

	/**
	 * 생성자 주입.
	 *
	 * <p>스프링이 등록된 빈 중에서 타입이 맞는 것을 찾아 자동으로 넣어준다.
	 * {@code AuthorRepository} 는 인터페이스인데, Spring Data JPA 가 만든
	 * 구현체 빈이 주입된다.
	 *
	 * @param authorRepository 저자 저장소
	 * @param bookRepository   책 저장소
	 */
	public LibraryService(AuthorRepository authorRepository, BookRepository bookRepository) {
		this.authorRepository = authorRepository;
		this.bookRepository = bookRepository;
	}

	// ========================================================================
	// 조회
	// ========================================================================

	/**
	 * 저자 목록을 조회한다. <b>책 목록은 포함하지 않는다.</b>
	 *
	 * <p>쿼리는 딱 1번만 나간다. LAZY 컬렉션을 건드리지 않기 때문이다.
	 * 목록 화면에서 책까지 필요 없다면 이게 가장 효율적이다.
	 *
	 * @return 저자 목록 (책 정보 없음)
	 */
	public List<AuthorResponse> findAllAuthors() {
		return authorRepository.findAll().stream()
				.map(AuthorResponse::withoutBooks)
				.toList();
	}

	/**
	 * 저자 목록을 <b>책까지 포함해서</b> 조회한다. — <b>N+1 이 발생하는 나쁜 예</b>
	 *
	 * <p>이 메서드는 일부러 비효율적으로 작성했다. 로그로 직접 확인해보라고 만든 것이다.
	 *
	 * <p>실행되는 쿼리:
	 * <pre>
	 *   SELECT * FROM authors;                     &lt;- 1번
	 *   SELECT * FROM books WHERE author_id = 1;   &lt;- 저자 수만큼 반복
	 *   SELECT * FROM books WHERE author_id = 2;
	 *   ...
	 * </pre>
	 *
	 * <p>{@code AuthorResponse.from()} 안에서 {@code author.getBooks()} 를 호출하는데,
	 * 이 컬렉션이 LAZY 라서 저자 한 명당 SELECT 가 한 번씩 추가로 나간다.
	 *
	 * <p>{@code application.properties} 에서 SQL 로그를 켜두었으니
	 * 애플리케이션 로그를 보면 쿼리가 줄줄이 찍히는 것을 눈으로 확인할 수 있다.
	 *
	 * @return 저자 목록 (책 포함)
	 * @see #findAllAuthorsWithBooksFetchJoin() 같은 결과를 쿼리 1번으로 얻는 버전
	 */
	public List<AuthorResponse> findAllAuthorsWithBooksNPlusOne() {
		return authorRepository.findAll().stream()
				.map(AuthorResponse::from) // <- 여기서 저자마다 추가 쿼리가 나간다
				.toList();
	}

	/**
	 * 저자 목록을 책까지 포함해서 조회한다. — <b>fetch join 으로 해결한 좋은 예</b>
	 *
	 * <p>위 메서드와 <b>결과는 완전히 같지만</b> 쿼리는 1번만 나간다.
	 * <pre>
	 *   SELECT DISTINCT a.*, b.* FROM authors a LEFT JOIN books b ON b.author_id = a.id;
	 * </pre>
	 *
	 * <p>이미 책 목록이 메모리에 채워진 상태로 넘어오므로,
	 * {@code getBooks()} 를 호출해도 추가 쿼리가 발생하지 않는다.
	 *
	 * <p>두 메서드를 번갈아 호출하면서 로그의 쿼리 개수를 비교해보는 것이
	 * 이 예제의 핵심 실습이다.
	 *
	 * @return 저자 목록 (책 포함)
	 */
	public List<AuthorResponse> findAllAuthorsWithBooksFetchJoin() {
		return authorRepository.findAllWithBooks().stream()
				.map(AuthorResponse::from) // 이미 로딩돼 있어서 추가 쿼리 없음
				.toList();
	}

	/**
	 * 저자 한 명을 조회한다.
	 *
	 * @param id 저자 ID
	 * @return 저자 (책 포함)
	 * @throws AuthorNotFoundException 해당 ID 의 저자가 없을 때
	 */
	public AuthorResponse findAuthor(Long id) {
		Author author = authorRepository.findById(id)
				// orElseThrow = 값이 없으면 예외를 던진다.
				// Optional 을 쓰면 "없을 때 무엇을 할지" 를 반드시 정하게 되어
				// null 체크를 깜빡하는 실수가 사라진다.
				.orElseThrow(() -> new AuthorNotFoundException(id));

		return AuthorResponse.from(author);
	}

	/**
	 * 특정 저자의 책 목록을 조회한다.
	 *
	 * @param authorId 저자 ID
	 * @return 책 목록
	 * @throws AuthorNotFoundException 해당 ID 의 저자가 없을 때
	 */
	public List<BookResponse> findBooksByAuthor(Long authorId) {
		// 존재하지 않는 저자면 빈 리스트가 아니라 404 를 주는 게 맞다.
		// "저자가 없다" 와 "저자는 있는데 책이 없다" 는 다른 상황이기 때문이다.
		if (!authorRepository.existsById(authorId)) {
			throw new AuthorNotFoundException(authorId);
		}

		return bookRepository.findByAuthorId(authorId).stream()
				.map(BookResponse::from)
				.toList();
	}

	// ========================================================================
	// 변경
	// ========================================================================

	/**
	 * 저자를 등록한다.
	 *
	 * <p>{@code @Transactional} 을 다시 붙여서 클래스 레벨의 {@code readOnly = true} 를 덮어쓴다.
	 * 이걸 빠뜨리면 읽기 전용 트랜잭션에서 INSERT 를 시도하게 되어 실패한다.
	 * <b>데이터를 바꾸는 메서드에는 반드시 붙인다.</b>
	 *
	 * @param request 등록 요청
	 * @return 등록된 저자
	 * @throws DuplicateEmailException 이미 같은 이메일의 저자가 있을 때
	 */
	@Transactional
	public AuthorResponse createAuthor(AuthorRequest request) {
		// 애플리케이션 레벨 중복 검사.
		// 사용자에게 친절한 메시지를 주기 위한 것이지, 이것만으로 안전한 건 아니다.
		// 두 요청이 동시에 들어오면 둘 다 이 검사를 통과할 수 있다(경쟁 조건).
		// 최종 방어선은 DB 의 unique 제약이다. (Author.email 의 unique = true)
		authorRepository.findByEmail(request.email()).ifPresent(existing -> {
			throw new DuplicateEmailException(request.email());
		});

		Author author = new Author(request.name(), request.email());

		// save() 가 INSERT 를 실행하고, DB 가 만든 id 를 객체에 채워준다.
		Author saved = authorRepository.save(author);

		return AuthorResponse.from(saved);
	}

	/**
	 * 특정 저자에게 책을 추가한다.
	 *
	 * <p><b>여기서 {@code bookRepository.save()} 를 호출하지 않는 것에 주목하라.</b>
	 * 두 가지가 함께 작동해서 저장이 일어난다:
	 * <ol>
	 *   <li><b>변경 감지(dirty checking)</b> — 트랜잭션 안에서 조회한 엔티티는
	 *       JPA 가 관리 상태로 들고 있다가, 트랜잭션이 끝날 때 변경된 부분을 찾아
	 *       자동으로 UPDATE/INSERT 를 실행한다.</li>
	 *   <li><b>{@code CascadeType.ALL}</b> — {@code Author.books} 에 걸어둔 설정 덕분에
	 *       저자에 딸린 새 책도 함께 저장된다.</li>
	 * </ol>
	 *
	 * <p>JPA 를 처음 보면 "저장 코드가 없는데 저장이 되는" 이 동작이 가장 낯설다.
	 * 반대로 말하면, <b>조회만 하려던 엔티티의 값을 무심코 바꾸면 그것도 DB 에 반영된다</b>는
	 * 뜻이므로 주의해야 한다. (그래서 조회 메서드에 {@code readOnly = true} 를 거는 것이다)
	 *
	 * @param authorId 저자 ID
	 * @param request  책 등록 요청
	 * @return 등록된 책
	 * @throws AuthorNotFoundException 해당 ID 의 저자가 없을 때
	 */
	@Transactional
	public BookResponse addBook(Long authorId, BookRequest request) {
		Author author = authorRepository.findById(authorId)
				.orElseThrow(() -> new AuthorNotFoundException(authorId));

		Book book = new Book(request.title(), request.price(), request.publishedDate());

		// 연관관계 편의 메서드. 양쪽 방향을 한 번에 맞춰준다.
		author.addBook(book);

		// 여기서 메서드가 끝나면 트랜잭션이 커밋되고, 그 시점에 INSERT 가 나간다.
		return BookResponse.from(book);
	}

	/**
	 * 저자를 삭제한다. 딸린 책들도 함께 삭제된다.
	 *
	 * <p>{@code CascadeType.ALL} + {@code orphanRemoval = true} 때문에
	 * 이 저자의 책들도 같이 지워진다.
	 *
	 * <p><b>실무에서는 이런 물리 삭제(hard delete)를 잘 쓰지 않는다.</b>
	 * 실수로 지우면 복구가 불가능하고, 통계나 정산 데이터가 함께 사라진다.
	 * 보통은 {@code deleted_at} 컬럼을 두고 값만 채우는
	 * <b>논리 삭제(soft delete)</b> 를 쓴다. 이 예제에서는 단순화를 위해 물리 삭제를 썼다.
	 *
	 * @param id 저자 ID
	 * @throws AuthorNotFoundException 해당 ID 의 저자가 없을 때
	 */
	@Transactional
	public void deleteAuthor(Long id) {
		Author author = authorRepository.findById(id)
				.orElseThrow(() -> new AuthorNotFoundException(id));

		authorRepository.delete(author);
	}

}
