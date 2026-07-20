package com.metionlabs.academy.dockers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 애플리케이션의 시작점.
 *
 * <p>{@code @SpringBootApplication} 하나가 사실 세 개의 애노테이션을 합친 것이다.
 * <ul>
 *   <li>{@code @SpringBootConfiguration} — 이 클래스가 설정 클래스임을 표시</li>
 *   <li>{@code @EnableAutoConfiguration} — 클래스패스에 있는 라이브러리를 보고 설정을 자동 구성.
 *       예를 들어 spring-boot-starter-webmvc 가 있으면 "웹 서버가 필요하겠구나" 하고
 *       내장 톰캣을 8080 포트로 띄워준다. 우리가 톰캣 설정을 한 줄도 안 썼는데 서버가 뜨는 이유다.</li>
 *   <li>{@code @ComponentScan} — <b>이 클래스가 속한 패키지와 그 하위</b>를 훑어서
 *       {@code @RestController}, {@code @Service} 같은 애노테이션이 붙은 클래스를 찾아 빈으로 등록</li>
 * </ul>
 *
 * <p><b>주의할 점:</b> 컴포넌트 스캔은 이 클래스의 패키지 기준으로 아래쪽만 훑는다.
 * 그래서 이 클래스는 항상 최상위 패키지에 둬야 한다.
 * 지금 구조가 그 규칙을 따른다:
 * <pre>
 *   com.metionlabs.academy.dockers            &lt;- 이 클래스 (최상위)
 *   com.metionlabs.academy.dockers.web        &lt;- HelloController (하위라서 스캔됨)
 * </pre>
 * 만약 이 클래스를 {@code ...dockers.config} 같은 하위로 옮기면
 * {@code web} 패키지가 스캔 범위 밖이 되어 컨트롤러가 등록되지 않고, 404 만 돌아온다.
 * 초보자가 자주 겪는 함정이다.
 */
@SpringBootApplication
public class SpringBootDockersApplication {

	/**
	 * 일반적인 자바 main 메서드다. 특별한 게 없다.
	 *
	 * <p>{@code SpringApplication.run()} 이 하는 일:
	 * <ol>
	 *   <li>스프링 컨테이너(ApplicationContext)를 생성</li>
	 *   <li>컴포넌트 스캔으로 빈들을 찾아 등록하고 의존성을 연결</li>
	 *   <li>내장 톰캣을 띄워 8080 포트를 리스닝</li>
	 *   <li>준비가 끝나면 요청을 받기 시작</li>
	 * </ol>
	 *
	 * <p>이 준비 과정이 몇 초 걸린다는 점이 도커에서 중요하다.
	 * 컨테이너 프로세스는 즉시 뜨지만 앱은 아직 요청을 받을 수 없는 구간이 존재한다.
	 * compose.yaml 의 healthcheck 가 바로 이 구간을 구분하기 위한 장치다.
	 *
	 * @param args 커맨드라인 인자. 스프링이 설정값으로 해석한다.
	 *             (예: {@code --server.port=9090} 으로 포트 변경 가능)
	 */
	public static void main(String[] args) {
		SpringApplication.run(SpringBootDockersApplication.class, args);
	}

}
