# metionlabs-academy

metionlabs 교육용 **example 프로젝트 모음**입니다.

각 폴더는 **하나의 주제를 다루는, 그 자체로 완결된 프로젝트**입니다.
1번부터 순서대로 따라가는 커리큘럼이 아니라, **필요한 주제의 폴더 하나만 열어서 바로 돌려보는** 구조입니다.

## 예제 목록

| 폴더 | 주제 | 스택 |
|------|------|------|
| [`spring-boot-dockers`](./spring-boot-dockers) | Spring Boot 애플리케이션을 도커라이즈해서 컨테이너로 띄우기 | Java 25 · Spring Boot 4.1 · Gradle(Kotlin DSL) · Docker |
| [`spring-data-jpas`](./spring-data-jpas) | Spring Data JPA로 MySQL 다루기 — 연관관계, 트랜잭션, N+1 문제 | Java 25 · Spring Boot 4.1 · JPA · MySQL 8.4 · Testcontainers |

## 폴더 네이밍 규칙

```
<스택>-<주제>
```

예: `spring-boot-dockers`, `spring-data-jpa`, `react-hooks`, `flutter-basics`

폴더 이름만 정렬해도 같은 스택끼리 묶이도록 하기 위한 규칙입니다.

## 새 예제를 추가할 때

1. 위 규칙에 맞는 폴더를 만듭니다.
2. 폴더 안에 **독립적으로 실행 가능한** 프로젝트를 넣습니다. 다른 폴더를 참조하지 않습니다.
3. 폴더 안에 `README.md`를 둡니다. 최소한 아래 3가지는 있어야 합니다.
   - 이 예제로 **무엇을 배우는지**
   - **실행 방법** (복붙하면 돌아가는 명령어)
   - **직접 해볼 것** (손으로 만져보는 실습 과제)
4. 이 파일의 예제 목록 표에 한 줄 추가합니다.

## 주의

이 저장소는 **공개(public)** 입니다.
비밀번호, API 키, 실제 서버 주소, 개인정보를 **절대 커밋하지 마세요.**
설정값이 필요하면 환경변수나 `.env.example` 같은 형태로 자리만 만들어 둡니다.
