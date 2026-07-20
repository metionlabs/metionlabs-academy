package com.metionlabs.academy.jpa.web.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * 책 등록 요청 DTO.
 *
 * <p>저자 ID 는 URL 경로에서 받으므로({@code POST /api/authors/{authorId}/books})
 * 이 DTO 에는 포함하지 않는다.
 *
 * <p>"리소스의 소속" 은 URL 로, "리소스의 내용" 은 본문으로 표현하는 것이
 * REST API 설계의 일반적인 관례다.
 *
 * @param title         제목
 * @param price         가격(원)
 * @param publishedDate 출간일
 */
public record BookRequest(

		@NotBlank(message = "제목은 필수입니다")
		@Size(max = 200, message = "제목은 200자를 넘을 수 없습니다")
		String title,

		// 숫자 타입에는 @NotBlank 를 쓸 수 없다. (문자열 전용이다)
		// 값이 안 넘어오면 null 이 되므로 @NotNull 로 막는다.
		@NotNull(message = "가격은 필수입니다")
		// @Min(0) = 0 이상. 음수 가격을 막는다.
		// 이런 "말이 안 되는 값" 은 DB 제약으로 막기 애매하므로 입력 단계에서 거른다.
		@Min(value = 0, message = "가격은 0원 이상이어야 합니다")
		Integer price,

		@NotNull(message = "출간일은 필수입니다")
		// @PastOrPresent = 과거 또는 오늘. 미래 날짜를 막는다.
		// 반대는 @Future (예: 예약 날짜, 만료일).
		// 날짜 검증은 잊기 쉬운데, 없으면 3025년 출간 도서가 등록된다.
		@PastOrPresent(message = "출간일은 미래일 수 없습니다")
		LocalDate publishedDate
		// JSON 에서는 "2026-07-20" 같은 문자열로 보내면
		// Jackson 이 LocalDate 로 알아서 변환해준다. (ISO-8601 형식)

) {
}
