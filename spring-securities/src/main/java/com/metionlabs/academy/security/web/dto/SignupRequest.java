package com.metionlabs.academy.security.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 DTO.
 *
 * <p>DTO 를 쓰는 이유(엔티티를 직접 안 받는 이유)는 spring-data-jpas 예제에 자세히 있다.
 * 여기서는 특히 <b>비밀번호가 절대 응답 DTO 로 나가지 않도록</b> 요청/응답 DTO 를 분리한 점이 중요하다.
 *
 * @param username 로그인 아이디
 * @param password 평문 비밀번호 (서버에서 해시해서 저장한다)
 */
public record SignupRequest(

		@NotBlank(message = "아이디는 필수입니다")
		@Size(min = 4, max = 50, message = "아이디는 4~50자여야 합니다")
		String username,

		@NotBlank(message = "비밀번호는 필수입니다")
		// 최소 길이를 강제한다. 짧은 비밀번호는 뚫기 쉽다.
		// 실무에서는 대소문자·숫자·특수문자 조합까지 요구하기도 한다(정규식 @Pattern).
		@Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다")
		String password

) {
}
