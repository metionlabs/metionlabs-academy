package com.metionlabs.academy.jpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 애플리케이션 시작점.
 *
 * <p>{@code @SpringBootApplication} 이 하는 일은 크게 세 가지다.
 * <ul>
 *   <li>설정 클래스 역할</li>
 *   <li><b>자동 구성</b> — 클래스패스를 보고 필요한 설정을 알아서 해준다</li>
 *   <li><b>컴포넌트 스캔</b> — 이 클래스의 패키지와 그 <b>하위</b>를 훑어 빈을 찾는다</li>
 * </ul>
 *
 * <p><b>이 프로젝트에서 자동 구성이 해주는 일:</b>
 * {@code spring-boot-starter-data-jpa} 와 MySQL 드라이버가 클래스패스에 있으니
 * 스프링이 이렇게 판단한다 — "JPA 를 MySQL 로 쓰려는구나".
 * 그래서 우리가 코드를 한 줄도 안 썼는데 아래가 전부 준비된다.
 * <ul>
 *   <li>{@code DataSource} (커넥션 풀 HikariCP 포함)</li>
 *   <li>{@code EntityManagerFactory} (JPA 핵심 객체)</li>
 *   <li>{@code TransactionManager} ({@code @Transactional} 이 동작하게 해주는 것)</li>
 *   <li>{@code JpaRepository} 인터페이스들의 <b>구현체 자동 생성</b></li>
 * </ul>
 * 접속 정보만 {@code application.properties} 에 적어주면 된다.
 *
 * <p><b>패키지 구조와 컴포넌트 스캔:</b>
 * <pre>
 *   com.metionlabs.academy.jpa              &lt;- 이 클래스 (최상위)
 *   com.metionlabs.academy.jpa.domain       &lt;- 엔티티
 *   com.metionlabs.academy.jpa.repository   &lt;- 저장소
 *   com.metionlabs.academy.jpa.service      &lt;- 업무 로직
 *   com.metionlabs.academy.jpa.web          &lt;- 컨트롤러
 *   com.metionlabs.academy.jpa.config       &lt;- 설정·초기화
 * </pre>
 * 이 클래스가 최상위에 있어야 하위 패키지가 전부 스캔된다.
 * 하위 패키지로 옮기면 스캔 범위를 벗어난 빈들이 등록되지 않아 404 나 주입 실패가 난다.
 */
@SpringBootApplication
public class SpringDataJpasApplication {

	/**
	 * @param args 커맨드라인 인자. 스프링이 설정값으로 해석한다.
	 *             (예: {@code --spring.profiles.active=docker})
	 */
	public static void main(String[] args) {
		SpringApplication.run(SpringDataJpasApplication.class, args);
	}

}
