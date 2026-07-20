package com.metionlabs.academy.jpa.web;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.metionlabs.academy.jpa.service.AuthorNotFoundException;
import com.metionlabs.academy.jpa.service.DuplicateEmailException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 모든 컨트롤러에서 발생한 예외를 한곳에서 HTTP 응답으로 바꿔주는 클래스.
 *
 * <p><b>이게 없으면 어떻게 되는가?</b>
 * 예외가 그대로 밖으로 나가서 스프링 기본 에러 페이지가 응답된다.
 * 상태 코드는 대개 500(서버 오류)이고, 응답에 <b>예외 클래스명과 스택 트레이스</b>가
 * 그대로 포함될 수 있다. 이건 두 가지로 나쁘다.
 * <ul>
 *   <li>클라이언트가 원인을 알 수 없다 — "저자가 없어서" 인지 "서버가 터져서" 인지 구분 불가</li>
 *   <li><b>보안</b> — 내부 패키지 구조, 사용 중인 라이브러리, 쿼리문이 공격자에게 노출된다</li>
 * </ul>
 *
 * <p>{@code @RestControllerAdvice} = {@code @ControllerAdvice} + {@code @ResponseBody}.
 * 모든 컨트롤러에 공통으로 적용되며, 반환값을 JSON 으로 만들어준다.
 *
 * <p><b>예외를 각 컨트롤러에서 {@code try-catch} 하지 않는 이유:</b>
 * 같은 처리를 모든 메서드에 반복해야 하고, 하나라도 빠뜨리면 그 API 만 응답 형식이 달라진다.
 * 여기 한곳에 모아두면 응답 형식이 자동으로 통일된다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 저자를 찾지 못한 경우 → <b>404 Not Found</b>
	 *
	 * <p>{@code @ExceptionHandler} = "이 타입의 예외가 올라오면 이 메서드가 처리한다".
	 *
	 * @param ex 발생한 예외
	 * @return 404 응답
	 */
	@ExceptionHandler(AuthorNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNotFound(AuthorNotFoundException ex) {
		return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	/**
	 * 이메일이 중복된 경우 → <b>409 Conflict</b>
	 *
	 * @param ex 발생한 예외
	 * @return 409 응답
	 */
	@ExceptionHandler(DuplicateEmailException.class)
	public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateEmailException ex) {
		return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
	}

	/**
	 * {@code @Valid} 검증에 실패한 경우 → <b>400 Bad Request</b>
	 *
	 * <p>이 예외는 우리가 던지는 게 아니라 <b>스프링이 자동으로</b> 던진다.
	 * {@code @Valid} 가 붙은 파라미터의 검증이 실패하면 발생한다.
	 *
	 * <p>{@code ex.getBindingResult()} 안에 어떤 필드가 왜 실패했는지가 다 들어 있다.
	 * 이걸 그냥 버리고 "잘못된 요청입니다" 라고만 응답하면
	 * 클라이언트는 뭘 고쳐야 할지 알 수 없다. 필드별 메시지를 꺼내서 돌려준다.
	 *
	 * <p>응답 예시:
	 * <pre>
	 *   {
	 *     "status": 400,
	 *     "message": "입력값이 올바르지 않습니다",
	 *     "errors": {
	 *       "name": "저자 이름은 필수입니다",
	 *       "email": "이메일 형식이 올바르지 않습니다"
	 *     }
	 *   }
	 * </pre>
	 *
	 * @param ex 발생한 예외
	 * @return 400 응답 (필드별 오류 포함)
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		// 필드명 -> 오류 메시지 형태로 모은다.
		Map<String, String> errors = new LinkedHashMap<>();

		ex.getBindingResult().getFieldErrors().forEach(fieldError ->
				// 같은 필드에 여러 오류가 있으면 먼저 담긴 것이 남는다.
				// (merge 없이 put 하면 나중 것이 덮어쓴다. 어느 쪽이든 큰 차이는 없다)
				errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));

		Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다");
		body.put("errors", errors);

		return ResponseEntity.badRequest().body(body);
	}

	// ------------------------------------------------------------------------
	// 내부 헬퍼
	// ------------------------------------------------------------------------

	/**
	 * 공통 형식의 에러 응답을 만든다.
	 *
	 * <p>응답 형식을 통일해두면 클라이언트가 파싱 코드를 한 번만 짜면 된다.
	 * API 마다 에러 형식이 다르면 그때마다 분기 처리를 해야 해서 괴롭다.
	 *
	 * @param status  HTTP 상태
	 * @param message 사람이 읽을 메시지
	 * @return 응답 엔티티
	 */
	private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(baseBody(status, message));
	}

	/**
	 * 모든 에러 응답이 공통으로 갖는 필드를 만든다.
	 *
	 * @param status  HTTP 상태
	 * @param message 사람이 읽을 메시지
	 * @return 응답 본문
	 */
	private Map<String, Object> baseBody(HttpStatus status, String message) {
		// 순서 유지를 위해 LinkedHashMap 사용 (응답 JSON 필드 순서가 매번 같아진다)
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", LocalDateTime.now().toString());
		body.put("status", status.value());
		body.put("error", status.getReasonPhrase());
		body.put("message", message);
		return body;
	}

}
