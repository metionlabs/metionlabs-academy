package com.metionlabs.academy.security.service;

/**
 * 로그인 실패(아이디 없음 또는 비밀번호 틀림) 시 던지는 예외.
 *
 * <p>HTTP <b>401 Unauthorized</b> 로 변환된다.
 *
 * <p><b>[보안] 메시지를 일부러 뭉뚱그렸다.</b>
 * "아이디가 없습니다" 와 "비밀번호가 틀렸습니다" 를 구분해 주면,
 * 공격자가 "이 아이디는 존재하는구나" 를 알아낼 수 있다(사용자 열거).
 * 그래서 실패 원인이 무엇이든 똑같이 "아이디 또는 비밀번호가 올바르지 않습니다" 로 응답한다.
 *
 * <p>참고: 401 과 403 의 차이
 * <ul>
 *   <li><b>401 Unauthorized</b> — "누구인지 모르겠다" (인증 실패/미인증). 로그인이 필요하다.</li>
 *   <li><b>403 Forbidden</b> — "누구인지는 알겠는데 권한이 없다" (인가 실패).
 *       예: USER 가 ADMIN 전용 API 에 접근. 로그인해도 안 된다.</li>
 * </ul>
 * 로그인 실패는 아직 신원을 확인 못 한 것이므로 401 이다.
 */
public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("아이디 또는 비밀번호가 올바르지 않습니다");
	}

}
