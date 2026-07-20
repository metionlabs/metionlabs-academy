package com.metionlabs.academy.jpa.web.dto;

import java.util.List;

import com.metionlabs.academy.jpa.domain.Author;

/**
 * 저자 응답 DTO.
 *
 * <p><b>왜 엔티티를 그대로 반환하지 않는가? — 응답 쪽 이유는 요청 쪽보다 더 절실하다.</b>
 *
 * <ol>
 *   <li><b>무한 재귀</b> — {@code Author} 는 {@code books} 를 갖고,
 *       {@code Book} 은 다시 {@code author} 를 갖는다(양방향 관계).
 *       엔티티를 그대로 JSON 으로 만들면
 *       저자 → 책 → 저자 → 책 → ... 을 끝없이 따라가다 {@code StackOverflowError} 로 죽는다.
 *       DTO 로 변환하면서 관계를 끊으면 이 문제가 원천적으로 사라진다.</li>
 *
 *   <li><b>의도치 않은 정보 노출</b> — 엔티티에 나중에 {@code password} 나
 *       {@code 주민번호} 필드가 추가되면, 그 순간 API 응답에 자동으로 딸려 나간다.
 *       아무도 코드를 고치지 않았는데 개인정보가 유출되는 것이다.
 *       DTO 는 <b>내보낼 것만 명시적으로 적으므로</b> 이런 사고가 구조적으로 불가능하다.</li>
 *
 *   <li><b>LazyInitializationException</b> — 컨트롤러가 엔티티를 반환하면
 *       JSON 변환은 트랜잭션이 끝난 뒤에 일어난다.
 *       그때 LAZY 필드를 건드리면 DB 연결이 이미 없어서 예외가 터진다.</li>
 * </ol>
 *
 * <p>정리하면: <b>엔티티는 컨트롤러 밖으로 내보내지 않는다.</b> 예외 없이 지키는 편이 좋다.
 *
 * @param id    저자 ID
 * @param name  이름
 * @param email 이메일
 * @param books 이 저자가 쓴 책 목록
 */
public record AuthorResponse(Long id, String name, String email, List<BookResponse> books) {

	/**
	 * 엔티티를 응답 DTO 로 변환한다. (책 목록 포함)
	 *
	 * <p><b>[중요] 이 메서드는 반드시 트랜잭션 안에서 호출해야 한다.</b>
	 * {@code author.getBooks()} 가 LAZY 컬렉션이라, 여기서 실제 DB 조회가 일어난다.
	 * 트랜잭션 밖이면 {@code LazyInitializationException} 이 터진다.
	 *
	 * <p>그래서 서비스 계층의 {@code @Transactional} 메서드 안에서 변환을 끝내고,
	 * 컨트롤러에는 이미 완성된 DTO 만 넘긴다.
	 *
	 * <p>{@code stream().map(...).toList()} 는 리스트의 각 원소를 변환해
	 * 새 리스트를 만드는 관용구다. for 문으로 써도 결과는 같다.
	 *
	 * @param author 저자 엔티티
	 * @return 응답 DTO
	 */
	public static AuthorResponse from(Author author) {
		return new AuthorResponse(
				author.getId(),
				author.getName(),
				author.getEmail(),
				author.getBooks().stream()
						.map(BookResponse::from) // 각 Book 을 BookResponse 로 변환
						.toList());
	}

	/**
	 * 책 목록 없이 저자 정보만 변환한다.
	 *
	 * <p>목록 조회처럼 <b>책까지는 필요 없는</b> 경우에 쓴다.
	 * 이걸 쓰면 LAZY 컬렉션을 건드리지 않으므로 추가 쿼리가 아예 발생하지 않는다.
	 * <b>N+1 문제를 피하는 가장 단순한 방법은, 애초에 필요 없는 데이터를 안 읽는 것이다.</b>
	 *
	 * @param author 저자 엔티티
	 * @return 책 목록이 빈 응답 DTO
	 */
	public static AuthorResponse withoutBooks(Author author) {
		return new AuthorResponse(
				author.getId(),
				author.getName(),
				author.getEmail(),
				List.of()); // 불변 빈 리스트
	}

}
