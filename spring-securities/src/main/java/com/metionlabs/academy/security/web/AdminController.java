package com.metionlabs.academy.security.web;

import java.util.List;

import com.metionlabs.academy.security.repository.UserRepository;
import com.metionlabs.academy.security.web.dto.UserResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 API — <b>인가(authorization)</b> 를 보여주는 엔드포인트.
 *
 * <p>이 경로({@code /api/admin/**})는 SecurityConfig 에서 {@code hasRole("ADMIN")} 으로 막았다.
 * <ul>
 *   <li>토큰이 없으면 → <b>401</b> (누구인지 모름)</li>
 *   <li>USER 권한 토큰이면 → <b>403</b> (누구인지는 알지만 권한 없음)</li>
 *   <li>ADMIN 권한 토큰이면 → <b>200</b> (통과)</li>
 * </ul>
 * 이 401 vs 403 의 차이를 직접 호출해보며 확인하는 것이 이 예제의 실습 포인트다.
 *
 * <p><b>권한 검사를 어디서 하나?</b> 이 예제는 SecurityConfig 에서 경로 기준으로 막았다.
 * 다른 방법으로, 메서드에 {@code @PreAuthorize("hasRole('ADMIN')")} 를 붙여
 * 메서드 단위로 막을 수도 있다(그러려면 @EnableMethodSecurity 필요).
 * 경로 기준은 한눈에 정책을 보기 좋고, 메서드 기준은 세밀한 제어에 좋다.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

	private final UserRepository userRepository;

	public AdminController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * 전체 사용자 목록 조회. (관리자만)
	 *
	 * <pre>
	 *   GET /api/admin/users
	 *   Authorization: Bearer &lt;ADMIN 토큰&gt;
	 * </pre>
	 *
	 * @return 전체 사용자 목록 (비밀번호 제외)
	 */
	@GetMapping("/users")
	public List<UserResponse> listUsers() {
		return userRepository.findAll().stream()
				.map(UserResponse::from)
				.toList();
	}

}
