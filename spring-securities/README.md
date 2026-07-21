# spring-securities

Spring Security로 **JWT 기반 인증/인가**를 구현하는 예제입니다.

- Java 25 (LTS) · Spring Boot 4.1.0 · Gradle 9.5.1 (Kotlin DSL)
- Spring Security + JWT (jjwt 0.12.6)
- MySQL 8.4 (도커 컨테이너) · Testcontainers

앞 예제 `spring-data-jpas`에서 배운 JPA 위에, "사용자를 DB에 저장하고 로그인시키는" 보안 계층을 얹습니다.

## 이 예제로 배우는 것

1. **인증 vs 인가** — "너 누구야(401)"와 "너 이거 할 수 있어(403)"의 차이
2. **JWT** — 토큰이 무엇인지, 왜 서명하는지, 왜 세션 대신 쓰는지
3. **SecurityFilterChain** — 요청이 컨트롤러에 닿기 전 필터에서 무슨 일이 일어나는지
4. **커스텀 필터** — Authorization 헤더의 토큰을 읽어 SecurityContext를 채우는 필터 직접 구현
5. **비밀번호 해시** — BCrypt, salt, 왜 평문 저장이 재앙인지
6. **역할 기반 인가** — `hasRole("ADMIN")`, `ROLE_` 접두사의 함정
7. **보안 예외 처리** — 401/403이 왜 `@RestControllerAdvice`로 안 잡히는지

> **학습용 구현입니다.** 이 예제는 JWT 필터를 직접 짜서 **원리**를 배웁니다. 실무에서는 Spring Security의 `oauth2-resource-server`나 Keycloak·Auth0 같은 인증 서버(IdP)를 쓰기도 합니다. 또 실무 JWT는 보통 access token(짧게) + refresh token(길게)을 쓰는데, 여기서는 개념 집중을 위해 access token 하나만 다룹니다.

## 실행 방법

### 1. 띄우기

```bash
docker compose up --build
```

MySQL이 준비된 뒤 앱이 뜨고, 초기 계정 두 개가 자동 생성됩니다.

| 아이디 | 비밀번호 | 권한 |
|--------|----------|------|
| `admin` | `admin1234` | ADMIN |
| `user` | `user1234` | USER |

### 2. 전체 흐름 따라가기

```bash
# (1) 회원가입 — 새 사용자는 항상 USER 권한
curl -X POST http://localhost:8080/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"username":"hong","password":"password123"}'

# (2) 로그인 → 토큰 발급
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"hong","password":"password123"}'
# 응답: {"accessToken":"eyJhbGci...","tokenType":"Bearer"}

# (3) 토큰을 변수에 저장 (아래 명령들에서 재사용)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"hong","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# (4) 토큰으로 내 정보 조회
curl http://localhost:8080/api/me -H "Authorization: Bearer $TOKEN"
```

### 3. 끄기

```bash
docker compose down       # 끄기 (DB 데이터 유지)
docker compose down -v    # 끄고 데이터까지 초기화
```

## 직접 해볼 것

### 1. 401 vs 403을 몸으로 구분하기 ★ 가장 중요

```bash
# 토큰 없이 보호된 API → 401 (누구인지 모름)
curl -i http://localhost:8080/api/me

# USER 토큰으로 관리자 API → 403 (누구인지는 알지만 권한 없음)
curl -i http://localhost:8080/api/admin/users -H "Authorization: Bearer $TOKEN"

# admin으로 로그인해서 다시 시도 → 200
ATOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin1234"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
curl -i http://localhost:8080/api/admin/users -H "Authorization: Bearer $ATOKEN"
```

**401은 "로그인하라", 403은 "로그인해도 넌 안 돼"** 입니다. 이 둘을 섞어 쓰면 클라이언트가 잘못된 안내를 하게 됩니다(예: 관리자 페이지에서 자꾸 재로그인 창을 띄움).

실측 결과:

| 요청 | 응답 |
|------|------|
| 토큰 없이 `/api/me` | **401** |
| USER 토큰으로 `/api/admin/users` | **403** |
| ADMIN 토큰으로 `/api/admin/users` | **200** |

### 2. 토큰 안을 열어보기

로그인해서 받은 토큰을 https://jwt.io 에 붙여넣어 보세요. 헤더와 페이로드(sub, role, exp)가 **그대로 읽힙니다.**

**이게 JWT의 핵심 감각입니다.** 토큰은 암호화가 아니라 인코딩일 뿐이라 누구나 내용을 봅니다. 그래서 **비밀번호 같은 민감 정보를 토큰에 넣으면 안 됩니다.** 대신 서명이 있어서 위조는 못 합니다 — 페이로드의 `USER`를 `ADMIN`으로 고쳐서 다시 요청해보면 서명이 안 맞아 401이 납니다.

### 3. 위조 토큰 막히는지 확인

```bash
curl -i http://localhost:8080/api/me -H "Authorization: Bearer eyJ.fake.token"
```

401이 납니다. `JwtTokenProvider`가 서명 검증에서 걸러냅니다. `application.properties`의 `jwt.secret`을 바꾸고 앱을 재시작하면, **이전에 발급한 토큰이 전부 무효**가 되는 것도 확인해보세요(서명 키가 달라졌으니까).

### 4. 비밀번호가 어떻게 저장되는지 DB에서 직접 보기

```bash
docker exec -it academy-security-mysql \
  mysql -u academy -pacademy1234 academy_security -e "SELECT username, password FROM users;"
```

password 컬럼이 `$2a$10$...`로 시작하는 **BCrypt 해시**입니다. 평문 `admin1234`는 어디에도 없습니다. 같은 비밀번호로 두 명을 만들어도 해시가 서로 다른 것(salt 때문)도 확인해보세요.

### 5. 토큰 만료 실험

`application.properties`의 `jwt.expiration-millis`를 `10000`(10초)으로 바꾸고 재시작하세요. 로그인 후 10초 뒤에 `/api/me`를 호출하면 401이 납니다. 만료 시간의 트레이드오프(보안 vs 편의)를 체감할 수 있습니다.

## API 목록

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| POST | `/api/auth/signup` | 불필요 | 회원가입 (항상 USER) |
| POST | `/api/auth/login` | 불필요 | 로그인 → 토큰 발급 |
| GET | `/api/me` | 필요 | 내 정보 (로그인한 사용자) |
| GET | `/api/admin/users` | ADMIN | 전체 사용자 목록 |
| GET | `/actuator/health` | 불필요 | 헬스체크 |

## 코드 읽는 순서 (추천)

1. `domain/User`, `domain/Role` — 무엇을 저장하는가
2. `jwt/JwtTokenProvider` — JWT를 만들고 검증하는 법 (**JWT가 뭔지 여기서**)
3. `jwt/JwtAuthenticationFilter` — 요청마다 토큰을 읽어 인증을 세팅 (**필터 체인**)
4. `config/SecurityConfig` — 전체 보안 규칙 (**이 예제의 심장**)
5. `service/AuthService` — 회원가입/로그인 로직 (비밀번호 해시·대조)
6. `web/*Controller` — 실제 엔드포인트

## 테스트

```bash
./gradlew test
```

Testcontainers가 진짜 MySQL을 자동으로 띄웁니다. 10개 테스트가 회원가입·로그인·401·403·위조 토큰·해시 저장을 검증합니다. **도커가 실행 중이어야 합니다.**

## 자주 만나는 문제

| 증상 | 원인 / 해결 |
|------|------|
| `hasRole("ADMIN")`인데 계속 403 | 저장된 권한이 `ROLE_ADMIN`이어야 합니다. `Role.toAuthority()`의 `ROLE_` 접두사를 확인하세요. |
| 커스텀 필터를 넣었더니 응답이 안 옴(hang) | 필터에서 `filterChain.doFilter(...)`를 빠뜨렸습니다. 반드시 다음으로 넘겨야 합니다. |
| 앱 기동 시 secret 관련 예외 | HS256은 최소 32자 키가 필요합니다. `jwt.secret`을 늘리세요. |
| 401/403이 `@RestControllerAdvice`에서 안 잡힘 | 정상입니다. 필터 단계 예외라 `SecurityErrorHandlers`가 처리합니다. |
| port 3306 충돌 | 이 예제 MySQL은 호스트 포트를 `13306`으로 씁니다. 앞 예제와 동시에 띄워도 됩니다. |
| `latest` 토큰이 다 만료됨 | `jwt.secret`을 바꾸면 기존 토큰이 전부 무효가 됩니다(서명 키가 달라져서). 정상입니다. |
