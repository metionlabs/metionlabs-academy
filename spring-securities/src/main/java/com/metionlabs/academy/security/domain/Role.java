package com.metionlabs.academy.security.domain;

/**
 * 사용자 권한(역할).
 *
 * <p>인증(Authentication)과 인가(Authorization)는 다르다. 자주 헷갈리는 개념이다.
 * <ul>
 *   <li><b>인증</b> — "너 누구야?" 로그인해서 신원을 확인하는 것</li>
 *   <li><b>인가</b> — "너 이거 할 수 있어?" 확인된 사용자가 특정 작업을 할 권한이 있는지</li>
 * </ul>
 * Role 은 <b>인가</b>를 위한 것이다. 같은 로그인 사용자라도 USER 냐 ADMIN 이냐에 따라
 * 접근할 수 있는 API 가 달라진다.
 *
 * <p><b>{@code enum} 으로 만든 이유:</b>
 * 권한을 문자열({@code "USER"}, {@code "ADMIN"})로 다루면 오타가 나도 컴파일러가 못 잡는다.
 * {@code "ADIMN"} 이라고 써도 그냥 통과되고, 권한 체크가 조용히 실패한다(= 아무도 접근 못 하거나 다 통과).
 * enum 은 정해진 값만 쓸 수 있어서 이런 실수를 컴파일 단계에서 막는다.
 */
public enum Role {

	/** 일반 사용자. */
	USER,

	/** 관리자. USER 가 할 수 있는 것에 더해 관리 기능에 접근할 수 있다. */
	ADMIN;

	/**
	 * Spring Security 가 기대하는 권한 문자열로 변환한다.
	 *
	 * <p><b>왜 "ROLE_" 접두사가 필요한가? — 이게 초심자를 가장 많이 괴롭히는 부분이다.</b>
	 * Spring Security 에는 두 가지 권한 개념이 있다.
	 * <ul>
	 *   <li><b>role(역할)</b> — {@code hasRole("ADMIN")} 으로 검사.
	 *       내부적으로는 자동으로 {@code "ROLE_"} 를 붙여 {@code "ROLE_ADMIN"} 을 찾는다.</li>
	 *   <li><b>authority(권한)</b> — {@code hasAuthority("ROLE_ADMIN")} 으로 검사.
	 *       접두사를 안 붙이고 그대로 비교한다.</li>
	 * </ul>
	 * 즉 {@code hasRole("ADMIN")} 이 동작하려면, 사용자에게 저장된 권한 이름이
	 * <b>반드시 {@code "ROLE_ADMIN"}</b> 이어야 한다. 접두사를 빠뜨리면
	 * "분명 ADMIN 인데 왜 403 이 뜨지?" 하며 몇 시간을 헤매게 된다.
	 *
	 * @return {@code "ROLE_" + 이름} (예: {@code "ROLE_ADMIN"})
	 */
	public String toAuthority() {
		return "ROLE_" + name();
	}

}
