package com.metionlabs.academy.jpa.web.dto;

import java.time.LocalDate;

import com.metionlabs.academy.jpa.domain.Book;

/**
 * 책 응답 DTO.
 *
 * @param id            책 ID
 * @param title         제목
 * @param price         가격(원)
 * @param publishedDate 출간일
 */
public record BookResponse(Long id, String title, Integer price, LocalDate publishedDate) {

	/**
	 * 엔티티를 응답 DTO 로 변환한다.
	 *
	 * <p><b>변환 메서드를 DTO 쪽에 두는 이유:</b>
	 * 반대로 엔티티에 {@code toResponse()} 를 두면, 엔티티가 웹 계층(DTO)을 알게 된다.
	 * 그러면 도메인이 API 스펙에 끌려다니게 되고,
	 * API 가 여러 개(웹용/앱용/관리자용)가 되면 엔티티에 변환 메서드가 계속 늘어난다.
	 *
	 * <p>의존 방향은 <b>웹 -> 도메인</b> 한 방향으로 유지하는 것이 좋다.
	 * DTO 는 엔티티를 알아도 되지만, 엔티티는 DTO 를 몰라야 한다.
	 *
	 * <p><b>[중요] 이 메서드는 반드시 트랜잭션 안에서 호출해야 한다.</b>
	 * 여기서는 LAZY 필드({@code book.getAuthor()})를 건드리지 않아서 괜찮지만,
	 * 만약 저자 이름을 포함시키려고 {@code book.getAuthor().getName()} 을 호출한다면
	 * 트랜잭션 밖에서는 {@code LazyInitializationException} 이 터진다.
	 *
	 * @param book 책 엔티티
	 * @return 응답 DTO
	 */
	public static BookResponse from(Book book) {
		return new BookResponse(
				book.getId(),
				book.getTitle(),
				book.getPrice(),
				book.getPublishedDate());
	}

}
