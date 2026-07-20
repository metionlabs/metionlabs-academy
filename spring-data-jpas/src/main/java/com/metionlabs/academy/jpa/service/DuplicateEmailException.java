package com.metionlabs.academy.jpa.service;

/**
 * 이미 등록된 이메일로 저자를 만들려 할 때 던지는 예외.
 *
 * <p>이 예외는 HTTP <b>409 Conflict</b> 로 변환된다.
 * ({@code GlobalExceptionHandler} 참고)
 *
 * <p><b>왜 400 이 아니라 409 인가?</b>
 * <ul>
 *   <li>400 Bad Request — 요청 형식 자체가 잘못됨 (이메일 형식이 아님, 필수값 누락)</li>
 *   <li>409 Conflict — 요청은 멀쩡한데 <b>현재 서버 상태와 충돌</b>함 (이미 존재함)</li>
 * </ul>
 * 클라이언트 입장에서 대응이 다르다.
 * 400 은 "입력을 고쳐서 다시 보내라", 409 는 "이미 있으니 다른 값을 쓰거나 기존 것을 써라".
 * 상태 코드를 정확히 주면 클라이언트가 알아서 적절한 안내를 할 수 있다.
 */
public class DuplicateEmailException extends RuntimeException {

	/**
	 * @param email 중복된 이메일
	 */
	public DuplicateEmailException(String email) {
		super("이미 등록된 이메일입니다. email=" + email);
	}

}
