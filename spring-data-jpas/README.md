# spring-data-jpas

Spring Data JPA로 **MySQL을 다루는 방법**을 배우는 예제입니다.

- Java 25 (LTS) · Spring Boot 4.1.0 · Gradle 9.5.1 (Kotlin DSL)
- MySQL 8.4 (도커 컨테이너)
- Testcontainers (테스트용 MySQL 자동 실행)

## 이 예제로 배우는 것

1. **엔티티 매핑** — 자바 클래스와 DB 테이블을 연결하기
2. **연관관계** — 1:N 관계, 연관관계의 주인, 양방향 매핑의 함정
3. **리포지토리** — 인터페이스만 만들면 구현체가 생기는 원리, 쿼리 메서드
4. **트랜잭션** — `@Transactional`, 변경 감지(dirty checking), `readOnly`의 의미
5. **N+1 문제** — 가장 흔한 JPA 성능 사고와 fetch join 해결법
6. **DTO 변환** — 엔티티를 그대로 반환하면 안 되는 3가지 이유
7. **컨테이너 2개 다루기** — 앱이 DB 준비를 기다리게 만들기

## 왜 MySQL을 도커로 띄우나요?

로컬에 직접 설치하지 않고 컨테이너로 띄웁니다. 이유는:

- **설치에 시간을 쓰지 않습니다.** 이 예제의 주제는 JPA지 MySQL 설치가 아닙니다. OS마다 다른 설치 과정, 포트 충돌, 한글 깨짐(charset)으로 반나절을 쓰는 대신 명령어 하나로 시작합니다.
- **마음껏 망가뜨릴 수 있습니다.** JPA를 배우다 보면 스키마가 반드시 꼬입니다. `docker compose down -v` 한 줄이면 3초 만에 처음 상태로 돌아옵니다. 겁내지 않고 실험할 수 있다는 게 학습에서 가장 큽니다.
- **모두가 같은 환경입니다.** 버전도 charset도 동일해서 "저만 안 돼요"가 생기지 않습니다.

이미 로컬에 MySQL이 설치돼 있다면 그걸 써도 됩니다. 아래 "로컬 MySQL 사용하기" 참고.

## 실행 방법

### 사전 준비

- Docker Desktop 설치 후 실행
- Java는 설치하지 않아도 됩니다 (빌드가 컨테이너 안에서 일어납니다)

### 1. 띄우기

```bash
docker compose up --build
```

MySQL 초기화까지 포함해 처음엔 1~2분 걸립니다. `app` 컨테이너는 MySQL이 준비될 때까지 **자동으로 기다립니다.**

### 2. 확인

샘플 데이터(저자 3명, 책 5권)가 자동으로 들어갑니다.

```bash
# 저자 목록 (책 정보 없음)
curl http://localhost:8080/api/authors

# 저자 1명 조회 (책 포함)
curl http://localhost:8080/api/authors/1

# 저자 등록
curl -X POST http://localhost:8080/api/authors \
  -H 'Content-Type: application/json' \
  -d '{"name":"신규저자","email":"new@example.com"}'

# 책 추가
curl -X POST http://localhost:8080/api/authors/1/books \
  -H 'Content-Type: application/json' \
  -d '{"title":"새 책","price":25000,"publishedDate":"2026-07-01"}'

# 검증 실패 (400과 필드별 메시지 확인)
curl -X POST http://localhost:8080/api/authors \
  -H 'Content-Type: application/json' \
  -d '{"name":"","email":"이건이메일이아님"}'
```

### 3. DB 직접 들여다보기

```bash
docker exec -it academy-mysql mysql -u academy -pacademy1234 academy
```

```sql
SHOW TABLES;
SELECT * FROM authors;
SELECT * FROM books;
DESC books;   -- author_id 외래키 컬럼 확인
```

DBeaver나 DataGrip 같은 GUI 툴로 붙어도 됩니다: `localhost:3306` / `academy` / `academy1234`

### 4. 끄기

```bash
docker compose down      # 끄기 (DB 데이터는 남음)
docker compose down -v   # 끄고 DB 데이터까지 삭제 (완전 초기화)
```

## 직접 해볼 것

### 1. N+1 문제를 눈으로 보기 ★ 가장 중요

먼저 로그 창을 하나 띄웁니다.

```bash
docker compose logs -f app
```

다른 터미널에서 두 엔드포인트를 각각 호출하고, 로그에 찍히는 **SELECT 개수를 세어보세요.**

```bash
# 나쁜 예 — 저자 수만큼 SELECT가 반복된다
curl http://localhost:8080/api/authors/with-books-n-plus-one

# 좋은 예 — SELECT가 딱 1번
curl http://localhost:8080/api/authors/with-books-fetch-join
```

샘플 데이터(저자 3명) 기준 실제 측정 결과입니다.

| 엔드포인트 | 실행된 SQL |
|---|---|
| `/api/authors/with-books-n-plus-one` | **4개** (저자 1 + 저자별 책 3) |
| `/api/authors/with-books-fetch-join` | **1개** |
| `/api/authors` (책 안 읽음) | 1개 |

저자가 100명이면 첫 번째는 **101개**가 됩니다.

**두 응답의 JSON은 완전히 같습니다.** 결과가 같은데 쿼리 수만 다릅니다.

이게 N+1 문제가 무서운 이유입니다. 테스트도 통과하고 화면도 정상이라 아무도 모르고 있다가, 운영에서 데이터가 쌓이면 그제서야 응답이 느려집니다. **눈으로 로그를 봐야만 발견됩니다.**

저자를 20명쯤 등록한 뒤 다시 해보면 차이가 훨씬 극적으로 보입니다.

### 2. LAZY와 EAGER 차이 체감하기

`Book.java`의 `@ManyToOne(fetch = FetchType.LAZY)`를 `FetchType.EAGER`로 바꾸고 다시 띄워보세요.

```bash
curl http://localhost:8080/api/authors/1/books
```

책만 조회했는데 저자 조회 쿼리가 따라붙습니다. 필요도 없는데 말이죠.
`@ManyToOne`의 **기본값이 EAGER**라서, 아무 생각 없이 쓰면 이 상태가 됩니다. 그래서 항상 LAZY로 바꿔 쓰는 게 정석입니다.

### 3. open-in-view의 함정 만나보기

`application.properties`에서 `spring.jpa.open-in-view=false`를 `true`로 바꾸고,
`LibraryService.findAuthor()`의 `@Transactional`을 잠시 지워보세요.

`false`일 때는 `LazyInitializationException`이 나지만 `true`면 동작합니다.
**"동작하니까 괜찮다"가 아닙니다.** true는 HTTP 응답이 끝날 때까지 DB 커넥션을 붙잡고 있어서, 트래픽이 몰리면 커넥션 풀이 고갈되고 서비스 전체가 멈춥니다. 원인 찾기도 어렵습니다.

### 4. 트랜잭션 롤백 확인하기

`LibraryService.addBook()` 마지막 줄 앞에 일부러 예외를 넣어보세요.

```java
if (true) throw new RuntimeException("일부러 터뜨림");
```

책이 저장되지 않고 롤백되는 것을 확인하세요. 그다음 이 예외를 `Exception`(Checked)으로 바꿔서 던져보면 **롤백되지 않는 것**도 확인해보세요. `@Transactional`은 기본적으로 Unchecked 예외에만 롤백합니다. 실무에서 데이터가 절반만 저장되는 사고의 흔한 원인입니다.

### 5. DB 없이 앱만 띄워보기

```bash
docker compose up app   # mysql 없이 app만
```

compose의 `depends_on`이 있어서 MySQL이 함께 뜹니다. `compose.yaml`에서 `depends_on` 블록을 지우고 MySQL을 끈 채로 앱만 띄우면 어떻게 되는지 보세요. 커넥션 오류로 기동에 실패합니다.

이번엔 `condition: service_healthy`만 지우고 `depends_on: [mysql]`로 바꿔보세요. 운이 나쁘면 앱이 MySQL보다 먼저 떠서 죽습니다. **"컨테이너 시작"과 "서비스 준비 완료"는 다릅니다.**

## 로컬 MySQL 사용하기

이미 로컬에 MySQL이 있다면 앱만 실행해도 됩니다.

```sql
CREATE DATABASE academy CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'academy'@'localhost' IDENTIFIED BY 'academy1234';
GRANT ALL PRIVILEGES ON academy.* TO 'academy'@'localhost';
```

```bash
./gradlew bootRun
```

`application.properties`의 기본값이 `localhost:3306`이라 그대로 붙습니다.
접속 정보가 다르면 환경변수로 덮어쓰면 됩니다.

```bash
SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/mydb" \
SPRING_DATASOURCE_USERNAME=root \
SPRING_DATASOURCE_PASSWORD=1234 \
./gradlew bootRun
```

## 테스트

```bash
./gradlew test
```

Testcontainers가 **테스트용 MySQL 컨테이너를 자동으로 띄웁니다.** 도커가 실행 중이어야 하고, 첫 실행은 이미지를 받느라 오래 걸립니다.

포트는 도커가 무작위로 배정하므로, compose로 DB를 띄워둔 상태에서도 충돌 없이 돌아갑니다.

## API 목록

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/authors` | 저자 목록 (책 없음) |
| GET | `/api/authors/with-books-n-plus-one` | 저자+책 — **N+1 발생 (학습용)** |
| GET | `/api/authors/with-books-fetch-join` | 저자+책 — **fetch join (학습용)** |
| GET | `/api/authors/{id}` | 저자 단건 (책 포함) |
| GET | `/api/authors/{id}/books` | 특정 저자의 책 목록 |
| POST | `/api/authors` | 저자 등록 |
| POST | `/api/authors/{id}/books` | 책 추가 |
| DELETE | `/api/authors/{id}` | 저자 삭제 (책도 함께) |

## 자주 만나는 문제

| 증상 | 원인 / 해결 |
|------|------|
| `port is already allocated` (3306) | 로컬 MySQL이 3306을 쓰고 있습니다. `compose.yaml`의 포트를 `"13306:3306"`으로 바꾸세요. |
| 앱이 `Connection refused`로 죽음 | MySQL이 아직 준비 안 된 상태입니다. `depends_on`의 `condition: service_healthy`가 있는지 확인하세요. |
| 한글이 `???`로 저장됨 | JDBC URL의 `characterEncoding=UTF-8`과 MySQL의 `utf8mb4` 설정을 확인하세요. |
| `Public Key Retrieval is not allowed` | JDBC URL에 `allowPublicKeyRetrieval=true`가 필요합니다. |
| `LazyInitializationException` | 트랜잭션 밖에서 LAZY 필드를 건드렸습니다. 서비스의 `@Transactional` 안에서 DTO로 변환하세요. |
| 스키마가 꼬임 | `docker compose down -v` 후 다시 띄우세요. 완전 초기화됩니다. |
| 테스트가 도커 오류로 실패 | Docker Desktop이 실행 중인지 확인하세요. Testcontainers는 도커가 필요합니다. |
