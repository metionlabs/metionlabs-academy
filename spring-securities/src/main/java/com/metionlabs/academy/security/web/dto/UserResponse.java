package com.metionlabs.academy.security.web.dto;

import com.metionlabs.academy.security.domain.User;

/**
 * 사용자 정보 응답.
 *
 * <p><b>[보안 핵심] 여기에 password 필드가 없다는 점을 반드시 보라.</b>
 * User 엔티티에는 비밀번호(해시)가 있지만, 이 응답 DTO 에는 담지 않는다.
 * 해시된 값이라도 밖으로 내보낼 이유가 전혀 없고, 노출 자체가 공격 표면을 넓힌다.
 *
 * <p>엔티티를 그대로 JSON 으로 반환했다면 password 가 자동으로 딸려 나갔을 것이다.
 * DTO 로 "내보낼 것만 명시" 하기 때문에 이런 사고가 구조적으로 막힌다.
 *
 * @param id       사용자 ID
 * @param username 아이디
 * @param role     권한 이름 (예: "USER", "ADMIN")
 */
public record UserResponse(Long id, String username, String role) {

	/**
	 * 엔티티를 응답 DTO 로 변환한다. (password 는 의도적으로 제외)
	 *
	 * @param user 사용자 엔티티
	 * @return 응답 DTO
	 */
	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getUsername(), user.getRole().name());
	}

}
