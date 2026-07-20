package com.metionlabs.academy.jpa;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 테스트용 MySQL 컨테이너 설정.
 *
 * <p>테스트를 실행하면 <b>진짜 MySQL 컨테이너가 자동으로 뜨고</b>,
 * 테스트가 끝나면 자동으로 정리된다. 우리가 직접 띄우거나 지울 필요가 없다.
 *
 * <p><b>{@code @ServiceConnection} 이 핵심이다.</b>
 * 이 애노테이션이 컨테이너의 접속 정보(호스트, 포트, 계정)를 읽어서
 * 스프링의 {@code DataSource} 설정에 <b>자동으로 연결</b>해준다.
 *
 * <p>예전에는 이걸 손으로 해야 했다:
 * <pre>
 *   &#64;DynamicPropertySource
 *   static void props(DynamicPropertyRegistry registry) {
 *       registry.add("spring.datasource.url", mysql::getJdbcUrl);
 *       registry.add("spring.datasource.username", mysql::getUsername);
 *       registry.add("spring.datasource.password", mysql::getPassword);
 *   }
 * </pre>
 * {@code @ServiceConnection} 하나로 이게 전부 대체된다.
 *
 * <p><b>포트가 겹치지 않는 이유:</b> Testcontainers 는 컨테이너를 띄울 때
 * 호스트의 비어 있는 포트를 무작위로 골라 매핑한다.
 * 그래서 로컬에 MySQL 이 이미 3306 을 쓰고 있어도, compose 로 DB 를 띄워둔 상태여도
 * 충돌 없이 테스트가 돌아간다.
 *
 * <p><b>{@code proxyBeanMethods = false}</b> 는 스프링이 이 설정 클래스를
 * 프록시로 감싸지 않게 해서 기동을 조금 빠르게 하는 최적화다.
 * 빈 메서드끼리 서로 호출하지 않는 단순한 설정 클래스라면 붙여도 안전하다.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	/**
	 * 테스트에서 사용할 MySQL 컨테이너.
	 *
	 * <p><b>이미지 태그를 {@code mysql:8.4} 로 고정한 것에 주목.</b>
	 * Spring Initializr 가 만들어준 기본값은 {@code mysql:latest} 였는데 그대로 두면 안 된다.
	 * <ul>
	 *   <li>운영·compose 에서 쓰는 버전과 달라지면 테스트의 의미가 줄어든다.
	 *       "운영과 같은 DB 로 테스트한다" 는 게 Testcontainers 를 쓰는 이유인데,
	 *       버전이 다르면 그 전제가 깨진다.</li>
	 *   <li>{@code latest} 는 시점에 따라 내용이 달라져서, 어제 통과하던 테스트가
	 *       오늘 갑자기 실패할 수 있다.</li>
	 * </ul>
	 * <b>compose.yaml 의 {@code image: mysql:8.4} 와 반드시 같은 값으로 유지한다.</b>
	 *
	 * @return MySQL 테스트 컨테이너
	 */
	// 참고: 예전 Testcontainers 에서는 MySQLContainer 가 제네릭이라
	// MySQLContainer<?> 나 MySQLContainer<SELF> 로 써야 했다.
	// 최신 버전(org.testcontainers:testcontainers-mysql)에서는 제네릭이 아니므로
	// 꺾쇠 없이 그냥 쓴다. 인터넷 예제를 복붙하면 여기서 컴파일 오류가 난다.
	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
	}

}
