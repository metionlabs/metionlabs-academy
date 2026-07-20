// ############################################################################
// Gradle 빌드 스크립트 (Kotlin DSL)
// ----------------------------------------------------------------------------
// gradle 기본기는 앞 예제(spring-boot-dockers/build.gradle.kts)에 자세히 적어두었다.
// 여기서는 이 예제에서 새로 추가된 의존성 위주로 설명한다.
// ############################################################################

plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.metionlabs.academy"
version = "0.0.1-SNAPSHOT"
description = "Spring Data JPA 로 MySQL 다루기 학습 예제"

java {
	// 최신 LTS. 누가 어디서 빌드해도 같은 자바 버전이 쓰이도록 고정한다.
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// ------------------------------------------------------------------------
	// 이 예제의 주인공
	// ------------------------------------------------------------------------
	// Spring Data JPA + Hibernate + 트랜잭션 관리 + HikariCP(커넥션 풀)가 한 번에 딸려온다.
	// 이거 하나 넣으면 JpaRepository 인터페이스만 만들어도 구현체가 자동 생성된다.
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")

	// 입력값 검증(@NotBlank, @Email 등)을 쓰기 위한 스타터.
	// [주의] 예전에는 web 스타터에 포함돼 있었지만 지금은 분리되어 있다.
	// 이걸 빠뜨리면 @Valid 를 붙여도 검증이 조용히 무시된다. 에러도 안 난다.
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// 웹 MVC + 내장 톰캣 + JSON 변환.
	// (Boot 3 까지의 이름은 spring-boot-starter-web 이었다)
	implementation("org.springframework.boot:spring-boot-starter-webmvc")

	// 헬스체크용. compose 의 healthcheck 가 /actuator/health/readiness 를 찌른다.
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	// ------------------------------------------------------------------------
	// MySQL 드라이버
	// ------------------------------------------------------------------------
	// runtimeOnly = 컴파일할 땐 필요 없고 실행할 때만 필요하다.
	//
	// 우리 코드는 JPA 인터페이스만 쓰고 MySQL 클래스를 직접 참조하지 않는다.
	// 그래서 컴파일에는 불필요하고, 실행 시점에만 있으면 된다.
	//
	// 이렇게 해두면 "실수로 MySQL 전용 클래스를 코드에 직접 쓰는 것" 도 막힌다.
	// 컴파일 자체가 안 되기 때문이다. DB 를 바꿀 여지를 남겨두는 효과가 있다.
	runtimeOnly("com.mysql:mysql-connector-j")

	// ------------------------------------------------------------------------
	// 테스트
	// ------------------------------------------------------------------------
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")

	// ------------------------------------------------------------------------
	// Testcontainers — 테스트 중에 진짜 MySQL 을 띄운다
	// ------------------------------------------------------------------------
	// [문제] JPA 를 테스트하려면 DB 가 필요한데, 어떤 DB 를 쓸 것인가?
	//
	//   1) 개발자 로컬 MySQL 에 붙인다
	//      -> 각자 데이터가 달라서 테스트 결과가 제각각. CI 서버에는 MySQL 이 없다.
	//
	//   2) H2 같은 메모리 DB 를 쓴다 (예전에 많이 쓰던 방법)
	//      -> 빠르지만 MySQL 과 문법·동작이 미묘하게 다르다.
	//         테스트는 통과하는데 운영에서 터지는 일이 생긴다.
	//         (예약어 처리, 날짜 함수, 대소문자 구분 등이 다르다)
	//
	//   3) Testcontainers -> 테스트 시작할 때 진짜 MySQL 컨테이너를 자동으로 띄우고,
	//      끝나면 자동으로 지운다. 운영과 같은 DB 로 테스트하니 신뢰할 수 있다.
	//
	// 이 예제는 어차피 도커가 필요하므로 3번이 자연스럽다.
	// [주의] 도커가 실행 중이어야 테스트가 돌아간다. 첫 실행은 이미지 다운로드로 느리다.
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-mysql")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	// JUnit 5 로 테스트를 실행한다. 없으면 테스트가 하나도 안 돌면서 조용히 성공한다.
	useJUnitPlatform()
}
