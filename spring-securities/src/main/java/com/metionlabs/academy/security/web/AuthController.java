package com.metionlabs.academy.security.web;

import com.metionlabs.academy.security.service.AuthService;
import com.metionlabs.academy.security.web.dto.LoginRequest;
import com.metionlabs.academy.security.web.dto.SignupRequest;
import com.metionlabs.academy.security.web.dto.TokenResponse;
import com.metionlabs.academy.security.web.dto.UserResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 공개 API. (회원가입, 로그인)
 *
 * <p>이 컨트롤러의 경로({@code /api/auth/**})는 SecurityConfig 에서
 * {@code permitAll()} 로 열어두었다. 로그인해야 토큰을 받는데, 토큰을 받으려면
 * 이 API 를 인증 없이 호출할 수 있어야 하기 때문이다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	/**
	 * 회원가입.
	 *
	 * <pre>
	 *   POST /api/auth/signup
	 *   {"username": "hong", "password": "password123"}
	 * </pre>
	 *
	 * @param request 가입 요청
	 * @return 201 Created + 가입된 사용자 정보 (비밀번호 제외)
	 */
	@PostMapping("/signup")
	public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
		UserResponse created = authService.signup(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	/**
	 * 로그인.
	 *
	 * <pre>
	 *   POST /api/auth/login
	 *   {"username": "hong", "password": "password123"}
	 *
	 *   응답: {"accessToken": "eyJhbGci...", "tokenType": "Bearer"}
	 * </pre>
	 *
	 * <p>받은 토큰은 이후 요청의 헤더에 이렇게 넣는다:
	 * <pre>
	 *   Authorization: Bearer eyJhbGci...
	 * </pre>
	 *
	 * @param request 로그인 요청
	 * @return 200 OK + 발급된 토큰
	 */
	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

}
