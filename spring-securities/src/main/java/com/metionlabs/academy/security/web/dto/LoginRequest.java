package com.metionlabs.academy.security.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO.
 *
 * <p>회원가입과 달리 길이 검증은 최소한만 둔다.
 * 로그인은 "형식이 맞는지" 보다 "실제로 일치하는지" 가 중요하고,
 * 형식 규칙(최소 8자 등)을 로그인에서까지 노출하면
 * 공격자에게 비밀번호 정책 힌트를 주는 셈이 된다.
 *
 * @param username 로그인 아이디
 * @param password 평문 비밀번호
 */
public record LoginRequest(

		@NotBlank(message = "아이디는 필수입니다")
		String username,

		@NotBlank(message = "비밀번호는 필수입니다")
		String password

) {
}
