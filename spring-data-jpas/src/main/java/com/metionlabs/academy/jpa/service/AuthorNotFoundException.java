package com.metionlabs.academy.jpa.service;

/**
 * 요청한 저자가 존재하지 않을 때 던지는 예외.
 *
 * <p><b>왜 그냥 {@code IllegalArgumentException} 을 쓰지 않고 새로 만드는가?</b>
 * <ul>
 *   <li>예외 이름 자체가 무슨 일이 일어났는지 설명해준다.
 *       로그에서 {@code AuthorNotFoundException} 을 보면 바로 상황을 알 수 있다.</li>
 *   <li>예외 종류별로 <b>다른 HTTP 상태 코드</b>를 매길 수 있다.
 *       (이 예외는 404, 중복 이메일은 409)</li>
 *   <li>나중에 "저자를 못 찾은 경우만" 특별히 처리하고 싶을 때 골라 잡을 수 있다.</li>
 * </ul>
 *
 * <p><b>{@code RuntimeException} 을 상속한 이유:</b>
 * 자바의 예외는 두 종류다.
 * <ul>
 *   <li><b>Checked</b>({@code Exception} 상속) — 반드시 {@code try-catch} 하거나
 *       {@code throws} 로 선언해야 한다. 안 하면 컴파일이 안 된다.</li>
 *   <li><b>Unchecked</b>({@code RuntimeException} 상속) — 강제하지 않는다.</li>
 * </ul>
 * "저자가 없다" 는 호출한 쪽이 그 자리에서 복구할 수 있는 문제가 아니다.
 * 어차피 위로 던져서 공통 처리기가 404 응답으로 바꿔야 한다.
 * 그런데 Checked 로 만들면 중간의 모든 메서드에 {@code throws} 가 줄줄이 붙어 코드만 지저분해진다.
 *
 * <p>추가로 <b>스프링의 트랜잭션 롤백 기본 동작</b>과도 관련이 있다.
 * {@code @Transactional} 은 <b>Unchecked 예외에만 자동으로 롤백</b>한다.
 * Checked 예외는 기본적으로 롤백하지 않아서,
 * 예외가 났는데도 데이터가 절반만 저장되는 사고가 날 수 있다.
 * 그래서 업무 예외는 Unchecked 로 만드는 것이 일반적이다.
 */
public class AuthorNotFoundException extends RuntimeException {

	/**
	 * @param id 찾지 못한 저자의 ID
	 */
	public AuthorNotFoundException(Long id) {
		// 메시지에 id 를 넣어두면 로그만 보고도 어떤 요청이 실패했는지 알 수 있다.
		// "저자를 찾을 수 없습니다" 라고만 적으면 디버깅할 때 아무 도움이 안 된다.
		super("저자를 찾을 수 없습니다. id=" + id);
	}

}
