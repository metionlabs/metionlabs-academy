package com.metionlabs.academy.security.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "내 정보" API — 로그인한 사용자만 접근할 수 있는 보호된 엔드포인트.
 *
 * <p>이 경로는 SecurityConfig 의 {@code anyRequest().authenticated()} 규칙에 걸린다.
 * 즉 <b>유효한 토큰이 없으면 401</b> 이 나고, 컨트롤러 코드는 아예 실행되지 않는다.
 *
 * <p>여기서 배우는 것: <b>"지금 요청한 사용자가 누구인지" 를 컨트롤러에서 꺼내는 법.</b>
 * JWT 필터가 SecurityContext 에 인증 정보를 심어두었기 때문에,
 * 컨트롤러는 토큰을 다시 파싱할 필요 없이 그 정보를 바로 받아 쓸 수 있다.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

	/**
	 * 현재 로그인한 사용자 정보를 돌려준다.
	 *
	 * <pre>
	 *   GET /api/me
	 *   Authorization: Bearer eyJhbGci...
	 * </pre>
	 *
	 * <p><b>현재 사용자를 얻는 두 가지 방법을 모두 보여준다:</b>
	 * <ol>
	 *   <li>{@code @AuthenticationPrincipal} — principal(우리가 필터에서 넣은 username)을 바로 주입</li>
	 *   <li>{@code Authentication} 파라미터 — 인증 객체 전체(권한 목록 포함)를 주입</li>
	 * </ol>
	 * 둘 다 스프링이 SecurityContext 에서 꺼내 자동으로 넣어준다.
	 * 컨트롤러 파라미터에 선언만 하면 된다.
	 *
	 * @param username       현재 사용자 아이디 (JWT 필터가 principal 로 넣어둔 값)
	 * @param authentication 인증 객체 전체
	 * @return 사용자 아이디와 권한
	 */
	@GetMapping
	public Map<String, Object> me(
			@AuthenticationPrincipal String username,
			Authentication authentication) {

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("username", username);
		// authorities = 이 사용자의 권한 목록. 예: ["ROLE_USER"]
		// authentication.getName() 도 username 과 같은 값을 준다.
		body.put("authorities", authentication.getAuthorities());
		return body;
	}

}
