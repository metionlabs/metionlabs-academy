package com.metionlabs.academy.jpa.web;

import java.net.URI;
import java.util.List;

import com.metionlabs.academy.jpa.service.LibraryService;
import com.metionlabs.academy.jpa.web.dto.AuthorRequest;
import com.metionlabs.academy.jpa.web.dto.AuthorResponse;
import com.metionlabs.academy.jpa.web.dto.BookRequest;
import com.metionlabs.academy.jpa.web.dto.BookResponse;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 저자·책 REST API.
 *
 * <p>컨트롤러는 <b>얇게</b> 유지한다. 하는 일은 딱 세 가지다.
 * <ol>
 *   <li>HTTP 요청을 받아 자바 객체로 변환</li>
 *   <li>서비스 호출</li>
 *   <li>결과를 HTTP 응답으로 변환</li>
 * </ol>
 * 업무 판단(중복 검사, 트랜잭션 묶기 등)은 전부 서비스가 한다.
 *
 * <p>컨트롤러에 로직이 들어가기 시작하면 배치·스케줄러에서 재사용할 수 없고,
 * 테스트할 때마다 HTTP 를 흉내내야 해서 번거로워진다.
 */
@RestController
@RequestMapping("/api/authors")
// 이 컨트롤러의 모든 경로 앞에 /api/authors 가 붙는다.
// 메서드마다 전체 경로를 반복해 적지 않아도 되고, 나중에 경로를 바꿀 때 한 곳만 고치면 된다.
public class LibraryController {

	private final LibraryService libraryService;

	public LibraryController(LibraryService libraryService) {
		this.libraryService = libraryService;
	}

	// ========================================================================
	// 조회
	// ========================================================================

	/**
	 * 저자 목록 조회. (책 정보 없음)
	 *
	 * <pre>
	 *   GET /api/authors
	 * </pre>
	 *
	 * @return 저자 목록
	 */
	@GetMapping
	public List<AuthorResponse> getAuthors() {
		return libraryService.findAllAuthors();
	}

	/**
	 * 저자 목록 조회 — <b>N+1 이 발생하는 버전</b> (학습용)
	 *
	 * <pre>
	 *   GET /api/authors/with-books-n-plus-one
	 * </pre>
	 *
	 * <p>호출한 뒤 애플리케이션 로그를 보면 SELECT 가 저자 수만큼 반복되는 것을 볼 수 있다.
	 * 아래 fetch join 버전과 <b>응답은 완전히 같지만 쿼리 수가 다르다.</b>
	 *
	 * <p>로그 보는 법: {@code docker compose logs -f app}
	 *
	 * @return 저자 목록 (책 포함)
	 */
	@GetMapping("/with-books-n-plus-one")
	public List<AuthorResponse> getAuthorsWithBooksNPlusOne() {
		return libraryService.findAllAuthorsWithBooksNPlusOne();
	}

	/**
	 * 저자 목록 조회 — <b>fetch join 으로 해결한 버전</b> (학습용)
	 *
	 * <pre>
	 *   GET /api/authors/with-books-fetch-join
	 * </pre>
	 *
	 * <p>쿼리가 1번만 나간다. 위 엔드포인트와 번갈아 호출하며 로그를 비교해보라.
	 *
	 * @return 저자 목록 (책 포함)
	 */
	@GetMapping("/with-books-fetch-join")
	public List<AuthorResponse> getAuthorsWithBooksFetchJoin() {
		return libraryService.findAllAuthorsWithBooksFetchJoin();
	}

	/**
	 * 저자 단건 조회.
	 *
	 * <pre>
	 *   GET /api/authors/1
	 * </pre>
	 *
	 * <p>{@code @PathVariable} = URL 경로의 일부를 파라미터로 받는다.
	 * {@code {id}} 자리에 들어온 값이 {@code Long id} 로 변환되어 전달된다.
	 * 숫자가 아닌 값이 오면 스프링이 자동으로 400 을 반환한다.
	 *
	 * @param id 저자 ID
	 * @return 저자 (책 포함)
	 */
	@GetMapping("/{id}")
	public AuthorResponse getAuthor(@PathVariable Long id) {
		return libraryService.findAuthor(id);
	}

	/**
	 * 특정 저자의 책 목록 조회.
	 *
	 * <pre>
	 *   GET /api/authors/1/books
	 * </pre>
	 *
	 * <p>URL 이 "1번 저자에 속한 책들" 이라는 관계를 그대로 드러낸다.
	 * {@code /api/books?authorId=1} 로도 만들 수 있지만,
	 * 종속 관계가 분명할 때는 경로로 표현하는 편이 의미가 잘 드러난다.
	 *
	 * @param id 저자 ID
	 * @return 책 목록
	 */
	@GetMapping("/{id}/books")
	public List<BookResponse> getBooksByAuthor(@PathVariable Long id) {
		return libraryService.findBooksByAuthor(id);
	}

	// ========================================================================
	// 등록 / 삭제
	// ========================================================================

	/**
	 * 저자 등록.
	 *
	 * <pre>
	 *   POST /api/authors
	 *   Content-Type: application/json
	 *
	 *   {"name": "김영한", "email": "kim@example.com"}
	 * </pre>
	 *
	 * <p><b>{@code @Valid} 가 핵심이다.</b> 이게 있어야 {@link AuthorRequest} 에 붙여둔
	 * {@code @NotBlank}, {@code @Email} 같은 검증이 실제로 실행된다.
	 * 빠뜨리면 <b>에러도 없이 검증이 통째로 무시</b>되어, 빈 이름이 그대로 DB 까지 간다.
	 * 초심자가 자주 겪는 함정이다.
	 *
	 * <p>검증에 실패하면 {@code MethodArgumentNotValidException} 이 발생하고,
	 * {@code GlobalExceptionHandler} 가 이를 400 응답으로 바꿔준다.
	 *
	 * <p><b>{@code @RequestBody}</b> = HTTP 요청 본문(JSON)을 자바 객체로 변환하라는 뜻.
	 * Jackson 이 JSON 의 키를 record 의 필드명과 맞춰서 채워준다.
	 *
	 * <p><b>왜 {@code ResponseEntity} 를 반환하는가?</b>
	 * 그냥 DTO 를 반환하면 상태 코드가 항상 200 OK 가 된다.
	 * 하지만 <b>새 리소스를 만든 경우엔 201 Created 가 정확한 표현</b>이다.
	 * 또한 {@code Location} 헤더에 "만들어진 리소스의 주소" 를 담아주는 것이 REST 관례다.
	 * 클라이언트가 그 주소로 바로 조회할 수 있다.
	 *
	 * @param request 저자 등록 요청
	 * @return 201 Created + 생성된 저자
	 */
	@PostMapping
	public ResponseEntity<AuthorResponse> createAuthor(@Valid @RequestBody AuthorRequest request) {
		AuthorResponse created = libraryService.createAuthor(request);

		// 생성된 리소스의 URI. 예: /api/authors/5
		URI location = URI.create("/api/authors/" + created.id());

		return ResponseEntity.created(location).body(created);
	}

	/**
	 * 특정 저자에게 책 추가.
	 *
	 * <pre>
	 *   POST /api/authors/1/books
	 *   Content-Type: application/json
	 *
	 *   {"title": "JPA 프로그래밍", "price": 43000, "publishedDate": "2015-07-27"}
	 * </pre>
	 *
	 * @param id      저자 ID
	 * @param request 책 등록 요청
	 * @return 201 Created + 생성된 책
	 */
	@PostMapping("/{id}/books")
	public ResponseEntity<BookResponse> addBook(
			@PathVariable Long id,
			@Valid @RequestBody BookRequest request) {

		BookResponse created = libraryService.addBook(id, request);

		return ResponseEntity
				.created(URI.create("/api/authors/" + id + "/books/" + created.id()))
				.body(created);
	}

	/**
	 * 저자 삭제. (딸린 책도 함께 삭제된다)
	 *
	 * <pre>
	 *   DELETE /api/authors/1
	 * </pre>
	 *
	 * <p><b>{@code 204 No Content} 를 반환하는 이유:</b>
	 * 삭제가 성공했으면 돌려줄 내용이 없다.
	 * 굳이 {@code {"success": true}} 같은 본문을 만들어 보내는 것보다,
	 * "성공했고 본문은 없다" 는 뜻의 204 가 정확하다.
	 *
	 * @param id 저자 ID
	 * @return 204 No Content
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteAuthor(@PathVariable Long id) {
		libraryService.deleteAuthor(id);

		return ResponseEntity.noContent().build();
	}

}
