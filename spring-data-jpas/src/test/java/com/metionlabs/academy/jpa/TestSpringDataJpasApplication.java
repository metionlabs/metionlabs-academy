package com.metionlabs.academy.jpa;

import org.springframework.boot.SpringApplication;

/**
 * 개발할 때 쓰는 실행용 클래스. (테스트가 아니다)
 *
 * <p><b>이게 왜 필요한가?</b>
 * 개발 중에 앱을 로컬에서 띄우려면 MySQL 이 필요하다.
 * 그때마다 {@code docker compose up mysql} 을 먼저 실행하는 게 번거롭다면,
 * 이 클래스를 실행하면 된다.
 *
 * <p>{@code SpringApplication.from(...).with(TestcontainersConfiguration.class)} 가
 * <b>MySQL 컨테이너를 자동으로 띄운 뒤 그 접속 정보로 앱을 실행</b>해준다.
 * 앱을 종료하면 컨테이너도 함께 정리된다.
 *
 * <p>정리하면 실행 방법이 세 가지 있다:
 * <ol>
 *   <li>{@code docker compose up --build} — 앱과 DB 모두 컨테이너로. <b>학습 기본 경로</b></li>
 *   <li>이 클래스 실행 — DB 만 컨테이너, 앱은 IDE 에서. 디버깅·코드 수정에 편하다</li>
 *   <li>{@code ./gradlew bootRun} — 앱도 DB 도 직접 준비. 로컬 MySQL 이 있어야 한다</li>
 * </ol>
 *
 * <p>2번이 개발 중에 가장 편하다. 코드를 고치고 IDE 에서 재시작하면 바로 반영되고,
 * 중단점(breakpoint)을 걸어 디버깅할 수도 있다.
 * 도커 이미지를 매번 다시 빌드할 필요가 없다.
 */
public class TestSpringDataJpasApplication {

	/**
	 * @param args 커맨드라인 인자
	 */
	public static void main(String[] args) {
		SpringApplication.from(SpringDataJpasApplication::main)
				.with(TestcontainersConfiguration.class)
				.run(args);
	}

}
