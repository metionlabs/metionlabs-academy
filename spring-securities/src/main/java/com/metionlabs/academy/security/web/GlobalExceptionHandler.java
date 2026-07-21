package com.metionlabs.academy.security.web;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.metionlabs.academy.security.service.DuplicateUsernameException;
import com.metionlabs.academy.security.service.InvalidCredentialsException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 컨트롤러에서 발생한 예외를 HTTP 응답으로 바꾼다.
 *
 * <p><b>[중요] 이 핸들러가 잡을 수 있는 예외의 범위</b>
 * {@code @RestControllerAdvice} 는 <b>컨트롤러 안에서 발생한 예외만</b> 잡는다.
 * 그런데 인증/인가 실패(401/403)는 요청이 컨트롤러에 닿기 <b>전에</b>,
 * 시큐리티 필터 체인 단계에서 발생한다.
 * <b>그래서 그 둘은 여기서 못 잡는다.</b> 401/403 은 SecurityConfig 에 등록한
 * AuthenticationEntryPoint / AccessDeniedHandler 가 처리한다.
 *
 * <p>여기서 잡는 것은 "컨트롤러까지 도달한" 예외들이다:
 * <ul>
 *   <li>로그인 실패(InvalidCredentials) — /api/auth/login 은 permitAll 이라 컨트롤러까지 온다</li>
 *   <li>아이디 중복(DuplicateUsername)</li>
 *   <li>입력 검증 실패(@Valid)</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 로그인 실패 → <b>401 Unauthorized</b>
	 */
	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
		return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	/**
	 * 아이디 중복 → <b>409 Conflict</b>
	 */
	@ExceptionHandler(DuplicateUsernameException.class)
	public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateUsernameException ex) {
		return build(HttpStatus.CONFLICT, ex.getMessage());
	}

	/**
	 * 입력 검증 실패 → <b>400 Bad Request</b> (필드별 메시지 포함)
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(fe ->
				errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));

		Map<String, Object> body = base(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다");
		body.put("errors", errors);
		return ResponseEntity.badRequest().body(body);
	}

	// ------------------------------------------------------------------------

	private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(base(status, message));
	}

	private Map<String, Object> base(HttpStatus status, String message) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", LocalDateTime.now().toString());
		body.put("status", status.value());
		body.put("error", status.getReasonPhrase());
		body.put("message", message);
		return body;
	}

}
