package com.metionlabs.academy.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 애플리케이션 시작점.
 *
 * <p>{@code @SpringBootApplication} 의 자동 구성이 이 프로젝트에서 해주는 일:
 * <ul>
 *   <li>클래스패스에 <b>spring-security</b> 가 있으니 → 보안 필터 체인을 자동으로 켠다.
 *       (우리는 SecurityConfig 에서 그 동작을 JWT 방식으로 커스터마이즈한다)</li>
 *   <li><b>data-jpa</b> + MySQL 드라이버가 있으니 → DataSource, JPA, 트랜잭션을 자동 구성한다</li>
 * </ul>
 *
 * <p>패키지 구조:
 * <pre>
 *   com.metionlabs.academy.security             &lt;- 이 클래스 (최상위, 컴포넌트 스캔 기준점)
 *   com.metionlabs.academy.security.domain      &lt;- User, Role
 *   com.metionlabs.academy.security.repository  &lt;- UserRepository
 *   com.metionlabs.academy.security.jwt         &lt;- 토큰 발급/검증, JWT 필터
 *   com.metionlabs.academy.security.config      &lt;- SecurityConfig 등 보안 설정
 *   com.metionlabs.academy.security.service     &lt;- AuthService (회원가입/로그인)
 *   com.metionlabs.academy.security.web         &lt;- 컨트롤러, DTO, 예외 처리
 * </pre>
 */
@SpringBootApplication
public class SpringSecuritiesApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringSecuritiesApplication.class, args);
	}

}
