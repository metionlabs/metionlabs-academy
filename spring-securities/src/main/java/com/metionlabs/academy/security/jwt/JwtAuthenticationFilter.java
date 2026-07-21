package com.metionlabs.academy.security.jwt;

import java.io.IOException;
import java.util.List;

import com.metionlabs.academy.security.domain.Role;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 모든 요청에서 <b>JWT 토큰을 검사해 "누가 요청했는지" 를 시큐리티에 알려주는</b> 필터.
 *
 * <p>=========================================================================
 * <p><b>필터 체인이란?</b>
 * <p>=========================================================================
 * HTTP 요청이 컨트롤러에 닿기 전에, 여러 개의 필터를 순서대로 통과한다.
 * <pre>
 *   요청 → [필터1] → [필터2] → ... → [이 JWT 필터] → ... → 컨트롤러
 * </pre>
 * Spring Security 는 이 체인에 여러 보안 필터를 꽂아둔다.
 * 우리는 그 사이에 "토큰을 읽어 인증 정보를 세팅하는" 필터를 하나 추가하는 것이다.
 *
 * <p><b>이 필터가 하는 일 (딱 이것뿐이다):</b>
 * <ol>
 *   <li>요청 헤더에서 토큰을 꺼낸다</li>
 *   <li>토큰이 유효하면, "이 사람은 인증된 아무개, 권한은 무엇" 을 시큐리티에 등록한다</li>
 *   <li>토큰이 없거나 이상하면, 아무것도 안 하고 그냥 다음으로 넘긴다</li>
 * </ol>
 *
 * <p><b>[중요] 이 필터는 "막는" 일을 하지 않는다.</b> 자주 오해하는 부분이다.
 * 토큰이 없어도 여기서 거부하지 않고 그냥 통과시킨다.
 * "그럼 누가 막나?" → 뒤쪽의 시큐리티 인가 단계가 막는다.
 * 이 필터는 그저 "인증 정보를 채워주는" 역할이고, 채워진 정보를 보고
 * 접근을 허용/거부하는 건 SecurityConfig 의 규칙과 시큐리티 내부 필터가 한다.
 * (역할을 이렇게 분리해두면, 어떤 경로는 인증 없이 허용하는 설정을 유연하게 할 수 있다)
 *
 * <p><b>{@code OncePerRequestFilter} 를 상속한 이유:</b>
 * 요청 하나가 내부적으로 다시 디스패치되는 경우(forward 등)에도
 * 이 필터가 <b>딱 한 번만</b> 실행되도록 보장해준다.
 * 그냥 Filter 를 구현하면 한 요청에 두 번 도는 상황이 생길 수 있다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	/** 토큰이 담겨 오는 헤더 이름. */
	private static final String AUTH_HEADER = "Authorization";

	/**
	 * 토큰 앞에 붙는 접두사. "Bearer " (뒤 공백 포함).
	 *
	 * <p>Bearer = "이 토큰을 소지한(bearer) 사람에게 권한을 준다" 는 인증 표준 방식.
	 * 실제 헤더는 이렇게 온다:  {@code Authorization: Bearer eyJhbGci...}
	 */
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider tokenProvider;

	/**
	 * @param tokenProvider 토큰 검증기
	 */
	public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
		this.tokenProvider = tokenProvider;
	}

	/**
	 * 요청마다 호출된다.
	 *
	 * @param request     들어온 요청
	 * @param response    나갈 응답
	 * @param filterChain 다음 필터로 넘기기 위한 체인
	 */
	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		String token = resolveToken(request);

		// 토큰이 있고 유효하면 인증 정보를 세팅한다.
		if (token != null && tokenProvider.validate(token)) {
			setAuthentication(token);
		}

		// [매우 중요] 어떤 경우든 반드시 다음 필터로 넘긴다.
		// 이 한 줄을 빠뜨리면 요청이 여기서 멈춰버려서 응답이 영영 안 온다(hang).
		// 초심자가 커스텀 필터를 짤 때 가장 많이 하는 실수다.
		filterChain.doFilter(request, response);
	}

	/**
	 * "이 사람은 인증된 아무개다" 를 시큐리티의 저장소(SecurityContext)에 등록한다.
	 *
	 * <p>여기에 등록해두면, 이후 시큐리티 인가 규칙과 컨트롤러가
	 * "지금 요청한 사용자가 누구고 권한이 뭔지" 를 알 수 있게 된다.
	 *
	 * @param token 검증을 통과한 토큰
	 */
	private void setAuthentication(String token) {
		String username = tokenProvider.getUsername(token);
		Role role = tokenProvider.getRole(token);

		// Authentication 객체 = "인증된 사용자" 를 표현하는 시큐리티의 표준 타입.
		//   1번째 인자: principal    - 누구인지 (여기선 username)
		//   2번째 인자: credentials  - 비밀번호. 이미 토큰으로 인증됐으므로 null.
		//   3번째 인자: authorities  - 권한 목록. 인가 검사에 쓰인다.
		//
		// [보안 주의] 권한 문자열은 반드시 토큰에서 꺼낸 값으로 세팅한다.
		// 토큰은 서명으로 위조가 막혀 있으므로 그 안의 role 을 믿을 수 있다.
		// 만약 요청 파라미터 같은 "클라이언트가 마음대로 넣을 수 있는" 값으로 권한을 정하면
		// 누구나 자기를 ADMIN 이라 우기며 권한 상승을 할 수 있다.
		var authentication = new UsernamePasswordAuthenticationToken(
				username,
				null,
				List.of(new SimpleGrantedAuthority(role.toAuthority())));

		// SecurityContextHolder = 현재 요청의 인증 정보를 담아두는 곳.
		// (요청마다 독립적으로 관리된다 — 스레드 단위 저장소라 다른 사용자와 섞이지 않는다)
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	/**
	 * 요청 헤더에서 순수한 토큰 문자열만 뽑아낸다.
	 *
	 * <p>{@code "Bearer eyJhbGci..."} 에서 {@code "Bearer "} 를 떼고 뒷부분만 반환한다.
	 *
	 * @param request 요청
	 * @return 토큰 (없으면 null)
	 */
	private String resolveToken(HttpServletRequest request) {
		String header = request.getHeader(AUTH_HEADER);

		// 헤더가 있고 "Bearer " 로 시작할 때만 유효한 형태로 본다.
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			return header.substring(BEARER_PREFIX.length());
		}

		return null;
	}

}
