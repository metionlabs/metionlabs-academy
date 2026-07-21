package com.metionlabs.academy.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 테스트용 MySQL 컨테이너 설정.
 *
 * <p>테스트를 실행하면 진짜 MySQL 컨테이너가 자동으로 뜬다. (도커가 실행 중이어야 함)
 * 자세한 설명은 spring-data-jpas 예제의 TestcontainersConfiguration 에 있다.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	// 이미지 태그는 compose.yaml 과 같은 mysql:8.4 로 고정한다.
	// (Initializr 기본값 mysql:latest 는 재현성이 깨지고, 최신 Testcontainers 에서
	//  MySQLContainer 는 비제네릭이라 <> 를 붙이면 컴파일 오류가 난다)
	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
	}

}
