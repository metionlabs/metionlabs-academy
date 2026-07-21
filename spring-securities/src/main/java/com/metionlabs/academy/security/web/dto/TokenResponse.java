package com.metionlabs.academy.security.web.dto;

/**
 * 로그인 성공 시 돌려주는 토큰 응답.
 *
 * <p>클라이언트는 이 토큰을 저장해두었다가, 이후 요청마다
 * {@code Authorization: Bearer <accessToken>} 헤더에 실어 보낸다.
 *
 * <p>{@code tokenType} 을 함께 주는 이유: 클라이언트가 헤더를 만들 때
 * "Bearer" 라는 접두사를 붙여야 한다는 것을 명시적으로 알려주기 위해서다.
 * OAuth2 표준 응답 형식과도 맞춘 것이다.
 *
 * @param accessToken 발급된 JWT
 * @param tokenType   토큰 종류 (항상 "Bearer")
 */
public record TokenResponse(String accessToken, String tokenType) {

	/**
	 * Bearer 타입 토큰 응답을 만든다.
	 *
	 * @param accessToken JWT 문자열
	 * @return 토큰 응답
	 */
	public static TokenResponse bearer(String accessToken) {
		return new TokenResponse(accessToken, "Bearer");
	}

}
