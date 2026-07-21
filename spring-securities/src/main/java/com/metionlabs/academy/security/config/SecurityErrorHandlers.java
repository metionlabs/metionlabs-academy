package com.metionlabs.academy.security.config;

import java.io.IOException;
import java.time.LocalDateTime;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 시큐리티 <b>필터 단계</b>에서 발생하는 인증/인가 실패를 JSON 응답으로 바꾼다.
 *
 * <p><b>왜 GlobalExceptionHandler 로는 안 되는가?</b>
 * 401/403 은 요청이 컨트롤러에 닿기 전, 필터 체인 안에서 결정된다.
 * {@code @RestControllerAdvice} 는 컨트롤러 예외만 잡으므로 여기에 관여할 수 없다.
 * 대신 시큐리티가 제공하는 두 개의 훅을 구현해서 응답을 직접 만든다.
 *
 * <ul>
 *   <li><b>AuthenticationEntryPoint</b> → <b>401</b>: 인증이 안 된(또는 토큰이 없는/잘못된) 요청이
 *       보호된 자원에 접근했을 때 호출된다. "너 누구야?"</li>
 *   <li><b>AccessDeniedHandler</b> → <b>403</b>: 인증은 됐지만 권한이 부족할 때 호출된다.
 *       예: USER 가 ADMIN 전용 API 접근. "누군지는 아는데 넌 안 돼."</li>
 * </ul>
 *
 * <p>이걸 등록하지 않으면 시큐리티 기본 동작이 나가는데, 형식이 우리 API 의
 * 나머지 에러 응답과 달라서 클라이언트가 일관되게 처리하기 어렵다. 그래서 통일해준다.
 */
@Component
public class SecurityErrorHandlers {

	/**
	 * 401 응답을 만드는 핸들러.
	 *
	 * @return AuthenticationEntryPoint
	 */
	public AuthenticationEntryPoint authenticationEntryPoint() {
		// 인증 실패 → 401. 메시지는 뭉뚱그린다(어떤 토큰이 왜 실패했는지 알려주면 공격에 도움).
		return (request, response, authException) ->
				writeError(response, HttpStatus.UNAUTHORIZED, "인증이 필요합니다");
	}

	/**
	 * 403 응답을 만드는 핸들러.
	 *
	 * @return AccessDeniedHandler
	 */
	public AccessDeniedHandler accessDeniedHandler() {
		// 인가 실패 → 403. 로그인은 됐으나 권한이 없는 경우.
		return (request, response, accessDeniedException) ->
				writeError(response, HttpStatus.FORBIDDEN, "접근 권한이 없습니다");
	}

	/**
	 * 응답 스트림에 JSON 에러 본문을 직접 쓴다.
	 *
	 * <p>필터 단계에서는 {@code @RestController} 처럼 객체를 반환하면 JSON 으로
	 * 바꿔주는 편의 기능이 없다. 그래서 상태 코드·콘텐츠 타입·본문을 손으로 세팅한다.
	 *
	 * @param response HTTP 응답
	 * @param status   상태 코드
	 * @param message  메시지
	 * @throws IOException 스트림 쓰기 오류
	 */
	private void writeError(HttpServletResponse response, HttpStatus status, String message)
			throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8"); // 한글 메시지가 깨지지 않도록

		// 라이브러리로 만들 수도 있지만, 의존성 없이 문자열로 직접 조립한다.
		// (필드가 몇 개 안 되고, 값에 따옴표 이스케이프가 필요한 사용자 입력이 없어서 안전하다)
		String body = """
				{"timestamp":"%s","status":%d,"error":"%s","message":"%s"}"""
				.formatted(
						LocalDateTime.now(),
						status.value(),
						status.getReasonPhrase(),
						message);

		response.getWriter().write(body);
	}

}
