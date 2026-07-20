package com.metionlabs.academy.jpa;

import java.time.LocalDate;
import java.util.List;

import com.metionlabs.academy.jpa.domain.Author;
import com.metionlabs.academy.jpa.domain.Book;
import com.metionlabs.academy.jpa.repository.AuthorRepository;
import com.metionlabs.academy.jpa.service.AuthorNotFoundException;
import com.metionlabs.academy.jpa.service.DuplicateEmailException;
import com.metionlabs.academy.jpa.service.LibraryService;
import com.metionlabs.academy.jpa.web.dto.AuthorRequest;
import com.metionlabs.academy.jpa.web.dto.AuthorResponse;
import com.metionlabs.academy.jpa.web.dto.BookRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 저자·책 기능 테스트.
 *
 * <p>테스트를 실행하면 Testcontainers 가 진짜 MySQL 컨테이너를 자동으로 띄운다.
 * <b>도커가 실행 중이어야 한다.</b> (첫 실행은 이미지 다운로드로 오래 걸린다)
 *
 * <p>애노테이션 설명:
 * <ul>
 *   <li>{@code @SpringBootTest} — 애플리케이션을 통째로 띄워서 테스트한다</li>
 *   <li>{@code @Import(TestcontainersConfiguration.class)} — MySQL 컨테이너 설정을 가져온다</li>
 *   <li>{@code @ActiveProfiles("test")} — test 프로파일로 실행.
 *       {@code DataInitializer} 에 {@code @Profile("!test")} 가 붙어 있어서
 *       <b>샘플 데이터가 들어가지 않는다.</b> 테스트는 자기 데이터를 직접 준비해야
 *       결과가 예측 가능해지기 때문이다.</li>
 *   <li>{@code @Transactional} — <b>각 테스트가 끝나면 자동으로 롤백된다.</b>
 *       테스트끼리 데이터가 섞이지 않고, DB 를 매번 청소할 필요도 없다.
 *       테스트에서 이 애노테이션은 "롤백해줘" 라는 뜻으로 동작한다는 점이
 *       운영 코드에서의 의미와 다르다.</li>
 * </ul>
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class SpringDataJpasApplicationTests {

	@Autowired
	private LibraryService libraryService;

	@Autowired
	private AuthorRepository authorRepository;

	/**
	 * 각 테스트 실행 전에 호출된다.
	 *
	 * <p>{@code @Transactional} 덕분에 자동 롤백되지만,
	 * 명시적으로 비워서 "이 테스트는 빈 상태에서 시작한다" 를 분명히 한다.
	 * 테스트는 <b>실행 순서에 상관없이 항상 같은 결과</b>가 나와야 한다.
	 */
	@BeforeEach
	void setUp() {
		authorRepository.deleteAll();
	}

	@Test
	@DisplayName("애플리케이션 컨텍스트가 정상적으로 로딩된다")
		// @DisplayName 을 쓰면 테스트 결과에 한글 설명이 표시되어
		// 무엇을 검증하는 테스트인지 한눈에 알 수 있다.
	void contextLoads() {
		// 이 메서드가 비어 있어도 의미가 있다.
		// 컨텍스트 로딩 자체(빈 등록, DB 연결, 엔티티 매핑)가 검증 대상이다.
		// 엔티티 매핑이 잘못됐다면 여기서 바로 실패한다.
		assertThat(libraryService).isNotNull();
	}

	@Test
	@DisplayName("저자를 등록하면 ID가 부여되고 조회할 수 있다")
	void createAuthor() {
		// given — 준비
		AuthorRequest request = new AuthorRequest("홍길동", "hong@example.com");

		// when — 실행
		AuthorResponse created = libraryService.createAuthor(request);

		// then — 검증
		// id 는 DB 가 만들어주므로 저장 전에는 null 이고 저장 후에는 값이 있어야 한다.
		assertThat(created.id()).isNotNull();
		assertThat(created.name()).isEqualTo("홍길동");
		assertThat(created.email()).isEqualTo("hong@example.com");
		// 방금 만든 저자는 아직 책이 없다.
		assertThat(created.books()).isEmpty();
	}

	@Test
	@DisplayName("이미 등록된 이메일로 저자를 만들면 예외가 발생한다")
	void createAuthorWithDuplicateEmail() {
		// given — 같은 이메일로 이미 한 명 등록해둔다
		libraryService.createAuthor(new AuthorRequest("홍길동", "same@example.com"));

		// when & then
		// assertThatThrownBy = "이 코드를 실행하면 예외가 나야 한다" 를 검증한다.
		// 예외가 안 나면 테스트가 실패한다.
		assertThatThrownBy(() ->
				libraryService.createAuthor(new AuthorRequest("임꺽정", "same@example.com")))
				.isInstanceOf(DuplicateEmailException.class)
				.hasMessageContaining("same@example.com");
		// 메시지에 이메일이 들어있는지도 확인한다.
		// 로그만 보고 원인을 알 수 있어야 하므로, 메시지 내용도 검증할 가치가 있다.
	}

	@Test
	@DisplayName("존재하지 않는 저자를 조회하면 예외가 발생한다")
	void findAuthorNotFound() {
		assertThatThrownBy(() -> libraryService.findAuthor(999999L))
				.isInstanceOf(AuthorNotFoundException.class);
	}

	@Test
	@DisplayName("저자에게 책을 추가하면 cascade 로 함께 저장된다")
	void addBook() {
		// given
		AuthorResponse author = libraryService.createAuthor(
				new AuthorRequest("김작가", "writer@example.com"));

		// when
		libraryService.addBook(author.id(),
				new BookRequest("첫 번째 책", 20000, LocalDate.of(2026, 1, 1)));

		// then
		// bookRepository.save() 를 호출한 적이 없는데도 저장된다.
		// Author.books 에 걸어둔 CascadeType.ALL 덕분이다.
		AuthorResponse found = libraryService.findAuthor(author.id());
		assertThat(found.books()).hasSize(1);
		assertThat(found.books().get(0).title()).isEqualTo("첫 번째 책");
		assertThat(found.books().get(0).price()).isEqualTo(20000);
	}

	@Test
	@DisplayName("fetch join 과 일반 조회는 쿼리 수만 다를 뿐 결과는 같다")
	void fetchJoinReturnsSameResultAsNPlusOne() {
		// given — 저자 2명, 각각 책 2권씩
		Author a1 = new Author("저자1", "a1@example.com");
		a1.addBook(new Book("책1", 10000, LocalDate.of(2026, 1, 1)));
		a1.addBook(new Book("책2", 20000, LocalDate.of(2026, 2, 1)));

		Author a2 = new Author("저자2", "a2@example.com");
		a2.addBook(new Book("책3", 30000, LocalDate.of(2026, 3, 1)));
		a2.addBook(new Book("책4", 40000, LocalDate.of(2026, 4, 1)));

		authorRepository.saveAll(List.of(a1, a2));

		// when — 같은 데이터를 두 가지 방법으로 조회
		List<AuthorResponse> nPlusOne = libraryService.findAllAuthorsWithBooksNPlusOne();
		List<AuthorResponse> fetchJoin = libraryService.findAllAuthorsWithBooksFetchJoin();

		// then — 결과는 동일해야 한다
		//
		// [이 테스트가 말하려는 것]
		// N+1 문제는 "결과가 틀리는" 문제가 아니라 "느린" 문제다.
		// 그래서 테스트로는 잡히지 않고, 눈으로 로그를 봐야만 발견된다.
		// 운영에서 터지기 전까지 아무도 모르는 이유가 여기에 있다.
		assertThat(nPlusOne).hasSize(2);
		assertThat(fetchJoin).hasSize(2);
		assertThat(fetchJoin)
				.extracting(AuthorResponse::name)
				.containsExactlyInAnyOrderElementsOf(
						nPlusOne.stream().map(AuthorResponse::name).toList());

		// 두 방식 모두 책 목록이 제대로 채워져 있어야 한다.
		assertThat(fetchJoin).allSatisfy(author ->
				assertThat(author.books()).hasSize(2));
	}

	@Test
	@DisplayName("저자를 삭제하면 그 저자의 책도 함께 삭제된다")
	void deleteAuthorCascadesToBooks() {
		// given
		AuthorResponse author = libraryService.createAuthor(
				new AuthorRequest("삭제될저자", "delete@example.com"));
		libraryService.addBook(author.id(),
				new BookRequest("삭제될 책", 15000, LocalDate.of(2026, 1, 1)));

		// when
		libraryService.deleteAuthor(author.id());

		// then
		assertThat(authorRepository.findById(author.id())).isEmpty();
		// 책도 함께 지워졌는지는 orphanRemoval + cascade 설정이 보장한다.
		assertThatThrownBy(() -> libraryService.findAuthor(author.id()))
				.isInstanceOf(AuthorNotFoundException.class);
	}

}
