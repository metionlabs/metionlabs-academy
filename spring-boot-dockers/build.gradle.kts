// ############################################################################
// Gradle 빌드 스크립트 (Kotlin DSL)
// ----------------------------------------------------------------------------
// 파일 확장자가 .kts = Kotlin Script. 예전에 많이 쓰던 build.gradle (Groovy) 대신
// 요즘 새로 만드는 프로젝트는 대체로 이쪽을 쓴다.
//
// Kotlin DSL 의 장점:
//   - IDE 자동완성이 제대로 동작한다 (Groovy 는 동적 언어라 한계가 있다)
//   - 오타를 실행 전에 컴파일 단계에서 잡아준다
//   - 함수/변수를 타고 들어가 정의를 볼 수 있다
// 단점: 첫 빌드가 Groovy 보다 조금 느리다 (스크립트도 컴파일하므로)
// ############################################################################

// plugins 블록 = 이 프로젝트에 어떤 기능을 추가할지 선언한다.
plugins {
	// 자바 컴파일, 테스트 실행, jar 생성 같은 기본 기능. 자바 프로젝트의 토대다.
	java

	// Spring Boot 플러그인.
	// 핵심 역할은 bootJar 태스크 제공 — 의존성 라이브러리까지 전부 담은
	// "실행 가능한 단일 jar"(fat jar)를 만들어준다.
	// 덕분에 java -jar app.jar 한 줄로 서버가 뜨고, 도커 이미지도 단순해진다.
	id("org.springframework.boot") version "4.1.0"

	// 의존성 버전을 자동으로 맞춰주는 플러그인.
	// 아래 dependencies 를 보면 버전 번호가 하나도 없는데, 이 플러그인이
	// Spring Boot 4.1.0 과 호환되는 조합을 알아서 골라준다.
	// 라이브러리 버전 충돌로 밤새우는 일을 막아주는 장치다.
	id("io.spring.dependency-management") version "1.1.7"
}

// 이 프로젝트의 좌표. 나중에 이 라이브러리를 배포하면 이 값으로 식별된다.
group = "com.metionlabs.academy"
version = "0.0.1-SNAPSHOT" // SNAPSHOT = 아직 개발 중인 버전이라는 관례적 표기
description = "Spring Boot 애플리케이션을 도커라이즈하는 학습 예제"

java {
	// ------------------------------------------------------------------------
	// toolchain = 이 프로젝트를 빌드할 자바 버전을 명시적으로 고정한다.
	// ------------------------------------------------------------------------
	// [중요] 이걸 안 쓰면 "빌드하는 사람의 JAVA_HOME" 에 따라 결과가 달라진다.
	// 내 노트북엔 21, 팀원은 17, CI 서버는 11 ... 이러면 재현이 안 된다.
	//
	// toolchain 을 지정하면 gradle 이 그 버전을 찾아 쓰고,
	// 없으면 자동으로 내려받기까지 한다. 누가 어디서 빌드해도 결과가 같아진다.
	//
	// Java 25 = 현재 최신 LTS(Long Term Support, 장기 지원) 버전.
	// LTS 가 아닌 버전은 6개월마다 지원이 끊기므로 실무에서는 보통 LTS 만 쓴다.
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

// 라이브러리를 어디서 받아올지.
// mavenCentral = 자바 생태계의 표준 공개 저장소.
repositories {
	mavenCentral()
}

dependencies {
	// ------------------------------------------------------------------------
	// "starter" 란?
	// ------------------------------------------------------------------------
	// 하나를 넣으면 관련 라이브러리 묶음이 통째로 딸려오는 꾸러미다.
	// 예전엔 spring-web, spring-webmvc, tomcat, jackson ... 을 하나씩 버전 맞춰
	// 적어야 했는데, starter 하나로 끝난다.

	// 헬스체크·메트릭 같은 운영용 엔드포인트(/actuator/**)를 제공한다.
	// 이 예제에서는 compose 의 healthcheck 가 찌를 /actuator/health/readiness 때문에 필요하다.
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	// 웹 MVC + 내장 톰캣 + JSON 변환(Jackson).
	// 참고: Spring Boot 3 까지는 이름이 spring-boot-starter-web 이었는데
	// Boot 4 에서 spring-boot-starter-webmvc 로 바뀌었다.
	// 인터넷의 옛날 예제를 복붙하면 여기서 막히니 주의.
	implementation("org.springframework.boot:spring-boot-starter-webmvc")

	// testImplementation = 테스트 코드를 컴파일/실행할 때만 쓰는 의존성.
	// 최종 산출물(jar)이나 도커 이미지에는 포함되지 않는다.
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")

	// testRuntimeOnly = 컴파일할 땐 필요 없고 실행할 때만 필요한 것.
	// JUnit 테스트를 실제로 구동시키는 엔진이다.
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// 모든 Test 타입 태스크에 공통 설정을 적용한다.
tasks.withType<Test> {
	// JUnit 5(Jupiter)로 테스트를 돌리겠다는 선언.
	// 이게 없으면 gradle 이 옛 JUnit 4 방식으로 돌리려 해서 테스트가 하나도 실행되지 않는다.
	// (에러도 안 나고 그냥 "0개 실행" 으로 조용히 넘어가서 눈치채기 어렵다)
	useJUnitPlatform()
}
