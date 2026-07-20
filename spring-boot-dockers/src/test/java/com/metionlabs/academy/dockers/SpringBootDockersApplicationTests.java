package com.metionlabs.academy.dockers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
// 주의: Spring Boot 4 에서 이 클래스의 패키지가 바뀌었다.
//   Boot 3 : org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
//   Boot 4 : org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
// 인터넷의 옛날 예제를 복붙하면 "package does not exist" 로 막히는 대표적인 지점이다.
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 애플리케이션 테스트.
 *
 * <p>{@code @SpringBootTest} 는 실제 애플리케이션을 통째로 띄워서 테스트한다.
 * 스프링 컨테이너를 만들고, 빈을 전부 등록하고, 의존성을 연결하는 것까지 진짜로 수행한다.
 * 그래서 느리지만, 실제 실행에 가장 가까운 상태를 검증할 수 있다.
 *
 * <p>{@code @AutoConfigureMockMvc} 는 MockMvc 를 쓸 수 있게 해준다.
 * MockMvc = 실제 톰캣을 띄우거나 네트워크를 타지 않고 컨트롤러에 가짜 HTTP 요청을 넣어보는 도구.
 * 진짜 서버를 안 띄우니 훨씬 빠르고, 포트 충돌 걱정도 없다.
 *
 * <p><b>이 테스트가 도커에서 갖는 의미:</b>
 * Dockerfile 의 빌드 단계에서 테스트를 돌리도록 바꾸면, 테스트가 깨진 코드는
 * 아예 이미지로 만들어지지 않게 막을 수 있다. (현재는 학습용이라 bootJar 만 실행한다)
 */
@SpringBootTest
@AutoConfigureMockMvc
class SpringBootDockersApplicationTests {

	/**
	 * 스프링이 만들어준 MockMvc 를 주입받는다.
	 *
	 * <p>테스트 클래스에서는 생성자 주입 대신 {@code @Autowired} 필드 주입을 써도 괜찮다.
	 * 테스트는 스프링이 직접 인스턴스를 만들어주고, 남이 재사용할 객체도 아니기 때문이다.
	 */
	@Autowired
	private MockMvc mockMvc;

	/**
	 * 애플리케이션 컨텍스트가 정상적으로 뜨는지 확인한다.
	 *
	 * <p>메서드 본문이 비어 있어서 "아무것도 안 하는 테스트" 처럼 보이지만 그렇지 않다.
	 * {@code @SpringBootTest} 가 컨텍스트를 띄우는 것 자체가 검증이다.
	 * 빈 설정이 잘못됐거나, 주입받을 수 없는 의존성이 있거나,
	 * {@code @Value("${academy.greeting}")} 의 설정값이 없으면 여기서 바로 실패한다.
	 *
	 * <p>가장 싸게 얻을 수 있는 안전망이라, 스프링 프로젝트에는 관례적으로 항상 둔다.
	 */
	@Test
	void contextLoads() {
	}

	/**
	 * GET / 이 200 OK 와 함께 기대한 JSON 필드들을 돌려주는지 확인한다.
	 *
	 * <p>검증 방식 설명:
	 * <ul>
	 *   <li>{@code perform(get("/"))} — "/" 로 GET 요청을 보낸 것처럼 흉내낸다</li>
	 *   <li>{@code status().isOk()} — 응답 코드가 200 인지</li>
	 *   <li>{@code jsonPath("$.hostname").exists()} — 응답 JSON 에 hostname 필드가 있는지.
	 *       {@code $} 는 JSON 의 최상위를 뜻한다.</li>
	 * </ul>
	 *
	 * <p><b>값이 아니라 존재만 검증하는 이유:</b>
	 * hostname 이나 uptime 은 실행할 때마다 달라진다.
	 * 이런 값을 특정 문자열과 비교하도록 테스트를 짜면, 코드는 멀쩡한데 테스트만 계속 깨진다.
	 * 그런 테스트는 결국 아무도 안 믿게 되고 무시된다. (flaky test 문제)
	 *
	 * <p>그래서 <b>변하는 값은 존재 여부만</b>, <b>변하지 않아야 하는 값은 정확히</b> 검증한다.
	 * greeting 은 설정에서 오는 고정값이므로 값까지 비교한다.
	 *
	 * @throws Exception MockMvc 의 perform 이 검사 예외를 던질 수 있어서 선언한다.
	 *                   테스트에서는 이런 예외를 잡아서 처리할 이유가 없으므로 그냥 던진다.
	 */
	@Test
	void rootReturnsRuntimeInfo() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			// application.properties 의 기본값이 그대로 주입됐는지.
			// (컨테이너에서는 compose 의 환경변수가 이 값을 덮어쓴다)
			.andExpect(jsonPath("$.greeting").value("Hello from metionlabs academy"))
			// 아래 값들은 실행 환경마다 달라지므로 "있는지" 만 본다.
			.andExpect(jsonPath("$.hostname").exists())
			.andExpect(jsonPath("$.javaVersion").exists())
			.andExpect(jsonPath("$.maxMemoryMb").exists());
	}

}
