package com.metionlabs.academy.security.config;

import com.metionlabs.academy.security.jwt.JwtAuthenticationFilter;
import com.metionlabs.academy.security.jwt.JwtProperties;
import com.metionlabs.academy.security.jwt.JwtTokenProvider;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 의 핵심 설정. <b>이 파일이 이 예제의 심장이다.</b>
 *
 * <p>Spring Security 스타터를 넣기만 하면 기본 동작이 켜진다:
 * "모든 요청에 로그인 요구 + 기본 로그인 폼 + 임시 비밀번호를 로그에 출력".
 * 우리는 이 파일에서 그 기본값을 <b>전부 걷어내고 JWT 방식으로 갈아끼운다.</b>
 *
 * <p>{@code @EnableConfigurationProperties(JwtProperties.class)} 는
 * JwtProperties 를 빈으로 등록해 주입받을 수 있게 한다.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	private final JwtTokenProvider jwtTokenProvider;
	private final SecurityErrorHandlers errorHandlers;

	public SecurityConfig(JwtTokenProvider jwtTokenProvider, SecurityErrorHandlers errorHandlers) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.errorHandlers = errorHandlers;
	}

	/**
	 * 보안 필터 체인 — "어떤 요청을 어떻게 통제할지" 의 규칙 전체.
	 *
	 * @param http 시큐리티 설정 빌더
	 * @return 구성된 필터 체인
	 * @throws Exception 설정 중 오류
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				// ----------------------------------------------------------------
				// (1) CSRF 비활성화
				// ----------------------------------------------------------------
				// CSRF 공격은 브라우저가 쿠키를 자동으로 실어 보내는 성질을 악용한다.
				// 우리는 쿠키/세션이 아니라 Authorization 헤더로 토큰을 보낸다.
				// 헤더는 브라우저가 자동으로 붙여주지 않으므로 CSRF 위험이 없다.
				// 따라서 꺼도 안전하다. (오히려 켜두면 API 호출이 막힌다)
				//
				// [주의] 세션·쿠키 기반 인증이라면 CSRF 를 절대 끄면 안 된다.
				.csrf(AbstractHttpConfigurer::disable)

				// ----------------------------------------------------------------
				// (2) 세션을 아예 안 만든다 (STATELESS)
				// ----------------------------------------------------------------
				// JWT 는 상태를 서버에 저장하지 않는 방식이다.
				// 이 설정으로 "세션을 만들지도, 쓰지도 마라" 라고 지시한다.
				// 이게 stateless 인증의 핵심이다. 서버가 여러 대여도 세션 공유가 필요 없다.
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// ----------------------------------------------------------------
				// (3) 경로별 접근 권한 규칙 — 인가(authorization)
				// ----------------------------------------------------------------
				// [매우 중요] 규칙은 위에서 아래로 검사되며, "먼저 걸리는 규칙" 이 이긴다.
				// 그래서 구체적인 규칙을 위에, 포괄적인 규칙(anyRequest)을 맨 아래에 둔다.
				// anyRequest 를 위로 올리면 그 아래 규칙은 영영 도달하지 못한다.
				.authorizeHttpRequests(auth -> auth
						// 회원가입·로그인은 인증 없이 누구나 접근 가능해야 한다.
						// (로그인하려면 토큰이 있어야 하는데, 토큰을 받으려면 로그인해야 하는
						//  닭-달걀 문제를 피하려면 이 경로는 열어둬야 한다)
						.requestMatchers("/api/auth/**").permitAll()

						// 헬스체크는 열어둔다. compose/로드밸런서가 인증 없이 찔러야 하므로.
						.requestMatchers("/actuator/health/**").permitAll()

						// 관리자 전용 경로. ADMIN 권한이 있어야 한다.
						// hasRole("ADMIN") 은 내부적으로 "ROLE_ADMIN" 권한을 찾는다.
						// (그래서 Role.toAuthority() 에서 "ROLE_" 접두사를 붙인 것이다)
						.requestMatchers("/api/admin/**").hasRole("ADMIN")

						// 공개 조회는 누구나. GET /api/notices 는 로그인 없이 볼 수 있게.
						.requestMatchers(HttpMethod.GET, "/api/notices/**").permitAll()

						// 그 외 모든 요청은 인증(로그인)만 되어 있으면 허용.
						// = 유효한 토큰만 있으면 접근 가능, 없으면 401.
						.anyRequest().authenticated())

				// ----------------------------------------------------------------
				// (4) 인증/인가 실패 시 응답을 우리 형식(JSON)으로 통일
				// ----------------------------------------------------------------
				// 이걸 등록하지 않으면 시큐리티 기본 응답이 나가서, 나머지 API 에러와 형식이 달라진다.
				//   authenticationEntryPoint → 401 (미인증)
				//   accessDeniedHandler      → 403 (권한 부족)
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(errorHandlers.authenticationEntryPoint())
						.accessDeniedHandler(errorHandlers.accessDeniedHandler()))

				// ----------------------------------------------------------------
				// (5) 우리가 만든 JWT 필터를 체인에 꽂는다
				// ----------------------------------------------------------------
				// UsernamePasswordAuthenticationFilter "앞에" 놓는다.
				// 그 필터는 폼 로그인(아이디/비번)을 처리하는 시큐리티 기본 필터인데,
				// 우리는 그 전에 토큰을 먼저 검사해서 인증을 끝내버리려는 것이다.
				// "토큰으로 이미 인증됐으면 폼 로그인 단계는 그냥 지나간다."
				.addFilterBefore(
						new JwtAuthenticationFilter(jwtTokenProvider),
						UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	/**
	 * 비밀번호 인코더.
	 *
	 * <p><b>BCrypt 를 쓴다.</b> 단순 해시(SHA-256 등)와 다른 두 가지 특징이 있다.
	 * <ul>
	 *   <li><b>salt 자동 포함</b> — 같은 비밀번호라도 매번 다른 해시가 나온다.
	 *       그래서 미리 계산해둔 해시표(레인보우 테이블)로 뚫을 수 없다.</li>
	 *   <li><b>일부러 느리다</b> — 해시 계산에 비용을 들이도록 설계됐다.
	 *       공격자가 초당 수억 개를 대입하는 무차별 공격을 어렵게 만든다.
	 *       (로그인은 어차피 사람이 하니 몇십 ms 느린 건 문제가 안 된다)</li>
	 * </ul>
	 *
	 * <p>이 빈은 회원가입 시 비번을 해시하고, 로그인 시 입력값과 저장된 해시를 대조하는 데 쓴다.
	 * 대조는 {@code encoder.matches(평문, 해시)} 로 한다. 저장된 해시를 복호화하는 게 아니라,
	 * 입력 평문을 같은 방식으로 해시해서 비교한다. (해시는 되돌릴 수 없으니까)
	 *
	 * @return BCrypt 인코더
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * 인증 매니저.
	 *
	 * <p>로그인 시 "이 아이디+비번이 맞는지" 를 실제로 대조하는 시큐리티의 엔진이다.
	 * {@code CustomUserDetailsService} 로 사용자를 찾고 {@code PasswordEncoder} 로 비번을 대조한다.
	 * 우리가 직접 만들 필요 없이, 시큐리티가 구성해둔 것을 꺼내 쓰기만 하면 된다.
	 *
	 * @param configuration 시큐리티가 제공하는 인증 설정
	 * @return 인증 매니저
	 * @throws Exception 구성 오류
	 */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
			throws Exception {
		return configuration.getAuthenticationManager();
	}

}
