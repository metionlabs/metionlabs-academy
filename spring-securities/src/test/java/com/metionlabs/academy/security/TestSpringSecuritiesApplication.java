package com.metionlabs.academy.security;

import org.springframework.boot.SpringApplication;

/**
 * 개발용 실행 클래스 — MySQL 컨테이너를 자동으로 띄운 채 앱을 실행한다.
 *
 * <p>IDE 에서 이걸 실행하면 docker compose 없이도 앱을 돌려볼 수 있다.
 * (자세한 설명은 spring-data-jpas 예제의 TestSpringDataJpasApplication 참고)
 */
public class TestSpringSecuritiesApplication {

	public static void main(String[] args) {
		SpringApplication.from(SpringSecuritiesApplication::main)
				.with(TestcontainersConfiguration.class)
				.run(args);
	}

}
