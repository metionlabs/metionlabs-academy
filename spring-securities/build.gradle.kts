// ############################################################################
// Gradle 빌드 스크립트 (Kotlin DSL)
// ----------------------------------------------------------------------------
// gradle 기본기는 spring-boot-dockers/build.gradle.kts 에 자세히 적어두었다.
// 여기서는 이 예제에서 새로 추가된 의존성(security, jjwt) 위주로 설명한다.
// ############################################################################

plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.metionlabs.academy"
version = "0.0.1-SNAPSHOT"
description = "Spring Security + JWT 인증/인가 학습 예제"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

// jjwt 버전을 한 곳에서 관리한다. 세 모듈(api/impl/jackson)의 버전을 맞춰야 하므로 변수로 뺐다.
val jjwtVersion = "0.12.6"

dependencies {
	// ------------------------------------------------------------------------
	// 이 예제의 주인공
	// ------------------------------------------------------------------------
	// Spring Security. 이거 하나만 넣어도 애플리케이션의 "모든 요청"이 인증을 요구하게 바뀐다.
	// 아무 설정도 안 하면 스프링이 임시 비밀번호를 로그에 찍고 기본 로그인 폼을 띄운다.
	// 우리는 SecurityConfig 에서 이 기본 동작을 JWT 방식으로 갈아끼운다.
	implementation("org.springframework.boot:spring-boot-starter-security")

	// 사용자 정보를 DB 에 저장하기 위한 JPA. (User 엔티티)
	// JPA 자체는 spring-data-jpas 예제에서 자세히 다뤘다.
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")

	// 회원가입/로그인 요청값 검증(@NotBlank 등).
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// 웹 MVC + 내장 톰캣 + JSON.
	implementation("org.springframework.boot:spring-boot-starter-webmvc")

	// 헬스체크용.
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	// ------------------------------------------------------------------------
	// jjwt — JWT 토큰을 만들고 검증하는 라이브러리
	// ------------------------------------------------------------------------
	// Spring Security 에는 JWT "발급" 기능이 없다. (검증은 oauth2-resource-server 로 되지만
	// 이 예제는 필터를 직접 짜서 원리를 배우는 게 목적이라 발급/검증 모두 jjwt 로 한다)
	//
	// 세 모듈로 나뉘어 있는 이유:
	//   jjwt-api     : 우리가 코드에서 쓰는 인터페이스 (컴파일 시 필요)
	//   jjwt-impl    : 실제 구현체 (실행 시에만 필요 -> runtimeOnly)
	//   jjwt-jackson : JSON 직렬화 (실행 시에만 필요). 스프링이 이미 쓰는 Jackson 을 연결한다
	// api 만 컴파일에 노출하고 구현은 감추는 구조라, 우리 코드가 구현 세부에 엮이지 않는다.
	implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

	// MySQL 드라이버. 실행 시에만 필요.
	runtimeOnly("com.mysql:mysql-connector-j")

	// ------------------------------------------------------------------------
	// 테스트
	// ------------------------------------------------------------------------
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")

	// 스프링 시큐리티 테스트 지원 (@WithMockUser 등 인증 상태를 흉내내는 도구).
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")

	// Testcontainers — 테스트 시 진짜 MySQL 을 자동으로 띄운다. (spring-data-jpas 참고)
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-mysql")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
