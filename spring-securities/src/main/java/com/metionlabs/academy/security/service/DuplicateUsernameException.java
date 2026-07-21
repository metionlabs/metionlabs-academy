package com.metionlabs.academy.security.service;

/**
 * 이미 존재하는 아이디로 회원가입을 시도할 때 던지는 예외.
 *
 * <p>HTTP <b>409 Conflict</b> 로 변환된다. (GlobalExceptionHandler 참고)
 * "요청은 올바른데 현재 상태와 충돌한다(이미 있다)" 는 의미다.
 *
 * <p>Unchecked 예외로 만든 이유는 spring-data-jpas 예제의
 * AuthorNotFoundException 주석에 자세히 설명돼 있다(트랜잭션 롤백 관련).
 */
public class DuplicateUsernameException extends RuntimeException {

	public DuplicateUsernameException(String username) {
		super("이미 사용 중인 아이디입니다. username=" + username);
	}

}
