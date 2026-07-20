package com.metionlabs.academy.dockers.web;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "지금 이 코드를 실행하고 있는 나는 누구인가" 를 응답으로 돌려주는 컨트롤러.
 *
 * <p>도커를 처음 배울 때 가장 중요한 감각은 이것이다:
 * <b>내 코드는 내 노트북이 아니라, 격리된 리눅스 컨테이너 안에서 돌고 있다.</b>
 *
 * <p>말로 들으면 당연한데 실감이 잘 안 난다. 그래서 이 컨트롤러는 추상적인 설명 대신
 * 실행 환경의 실제 값(호스트명, 자바 버전, 메모리 한도)을 그대로 보여준다.
 * 로컬에서 한 번, 컨테이너에서 한 번 띄워서 값을 비교해보면 차이가 눈에 보인다.
 *
 * <ul>
 *   <li>{@code ./gradlew bootRun} 으로 로컬 실행 → hostname 이 내 노트북 이름</li>
 *   <li>{@code docker compose up} 으로 컨테이너 실행 → hostname 이 컨테이너 ID</li>
 * </ul>
 */
@RestController // = @Controller + @ResponseBody.
                // 반환값을 뷰(HTML) 이름으로 해석하지 않고 응답 본문에 직접 쓴다.
                // Map 을 반환하면 Jackson 이 자동으로 JSON 으로 바꿔준다.
public class HelloController {

	/**
	 * 인사말. 코드에 박아넣지 않고 설정에서 주입받는다.
	 *
	 * <p>이렇게 해두면 이미지를 다시 빌드하지 않고도 환경변수로 값을 바꿀 수 있다.
	 * ({@code docker run -e ACADEMY_GREETING="안녕"})
	 */
	private final String greeting;

	/**
	 * 생성자 주입(constructor injection).
	 *
	 * <p>필드에 {@code @Autowired} 를 붙이는 방식보다 이 방식이 권장된다.
	 * <ul>
	 *   <li>필드를 {@code final} 로 만들 수 있다 → 생성 후 절대 안 바뀜이 보장된다</li>
	 *   <li>의존성 없이는 객체 생성 자체가 불가능하다 → 반쯤 만들어진 객체가 생기지 않는다</li>
	 *   <li>테스트에서 new 로 직접 만들기 쉽다 → 스프링 없이도 단위 테스트 가능</li>
	 * </ul>
	 *
	 * <p>생성자가 하나뿐이면 {@code @Autowired} 를 생략해도 스프링이 알아서 주입한다.
	 *
	 * @param greeting {@code academy.greeting} 설정값.
	 *        {@code @Value} 의 {@code ${...}} 는 "설정에서 이 키를 찾아 넣어라" 라는 뜻이다.
	 *        값을 못 찾으면 애플리케이션이 시작 단계에서 실패한다.
	 *        (오타를 런타임이 아니라 기동 시점에 잡아준다는 뜻이라, 오히려 안전한 동작이다)
	 */
	public HelloController(@Value("${academy.greeting}") String greeting) {
		this.greeting = greeting;
	}

	/**
	 * GET / 요청에 실행 환경 정보를 JSON 으로 돌려준다.
	 *
	 * <p>{@link LinkedHashMap} 을 쓴 이유: 일반 {@code HashMap} 은 키 순서를 보장하지 않아서
	 * 응답 JSON 의 필드 순서가 실행할 때마다 뒤바뀐다. 학습 자료에서는 출력이 매번 같아야
	 * 비교하기 좋으므로, 넣은 순서를 유지하는 LinkedHashMap 을 쓴다.
	 *
	 * @return 실행 환경 정보
	 */
	@GetMapping("/") // HTTP GET 메서드 + 경로 "/" 에 이 메서드를 연결한다.
	public Map<String, Object> hello() {
		Map<String, Object> body = new LinkedHashMap<>();

		// 설정에서 주입받은 값. compose.yaml 의 ACADEMY_GREETING 이 이걸 덮어쓴다.
		body.put("greeting", greeting);

		// 컨테이너 안이면 컨테이너 ID 가 나온다. 이 예제의 핵심 관전 포인트.
		body.put("hostname", hostname());

		// 내 맥에 JDK 21 이 깔려 있어도, 컨테이너 안이면 25 로 나온다.
		// "빌드/실행 환경이 이미지에 고정된다" 는 도커의 핵심 가치를 보여주는 값이다.
		// 팀원 노트북마다 자바 버전이 달라서 생기는 "내 컴퓨터에선 되는데요" 문제가 사라진다.
		body.put("javaVersion", Runtime.version().toString());

		// 컨테이너가 다시 시작되면 0 부터 다시 센다. 재시작 여부를 눈으로 확인하는 용도.
		body.put("uptime", uptime().toString());

		// 컨테이너에 CPU 제한을 걸면 이 숫자도 따라 내려간다.
		// (compose 의 deploy.resources.limits 에 cpus: '1.0' 을 추가해보면 확인 가능)
		body.put("availableProcessors", Runtime.getRuntime().availableProcessors());

		// [가장 중요한 값]
		// JVM 이 "내가 쓸 수 있는 최대 힙" 이라고 인식한 크기 (MB).
		// compose 에서 memory: 512M 을 걸었으면 그 75% 인 약 384MB 근처가 나와야 정상이다.
		// (Dockerfile 의 -XX:MaxRAMPercentage=75.0 이 동작한다는 증거)
		//
		// 만약 컨테이너 제한과 무관하게 훨씬 큰 값(호스트 메모리 기준)이 나온다면,
		// 그건 JVM 이 컨테이너 제한을 못 보고 있다는 뜻이고, 곧 OOMKilled 로 이어진다.
		//
		// 1024 * 1024 로 나누는 이유: maxMemory() 는 바이트 단위라서 MB 로 환산.
		body.put("maxMemoryMb", Runtime.getRuntime().maxMemory() / (1024 * 1024));

		return body; // Jackson 이 이 Map 을 JSON 으로 직렬화해서 응답 본문에 쓴다.
	}

	/**
	 * 이 프로세스가 돌고 있는 호스트의 이름을 구한다.
	 *
	 * <p>컨테이너 안에서는 도커가 컨테이너 ID 앞 12자리를 호스트명으로 설정하기 때문에
	 * {@code c6c8f25d8f16} 같은 값이 나온다. 로컬에서 그냥 실행하면 노트북 이름이 나온다.
	 *
	 * <p>{@code docker compose up --scale app=3} 으로 3개를 띄우면 세 컨테이너가 각각
	 * 다른 값을 돌려준다. "같은 이미지로 만든 컨테이너들도 서로 별개의 개체" 임을 보여준다.
	 *
	 * @return 호스트명. 조회 실패 시 {@code "unknown"}
	 */
	private String hostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException ex) {
			// 호스트명 조회 실패는 이 예제의 본질이 아니다.
			// 여기서 예외를 그대로 던지면 화면 전체가 500 에러가 되어 다른 값들도 못 보게 된다.
			// 부가 정보 하나 때문에 주요 기능을 망가뜨리지 않는 것 — 실무에서도 자주 하는 판단이다.
			return "unknown";
		}
	}

	/**
	 * JVM 이 시작된 뒤 흘러간 시간.
	 *
	 * <p>{@link ManagementFactory} 는 JVM 자신의 상태를 들여다보는 표준 API 다.
	 * {@code getUptime()} 이 밀리초를 주므로 {@link Duration} 으로 감싼다.
	 * Duration 의 {@code toString()} 은 ISO-8601 형식({@code PT2M24.9S} = 2분 24.9초)으로 나온다.
	 *
	 * @return 기동 후 경과 시간
	 */
	private Duration uptime() {
		return Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());
	}

}
