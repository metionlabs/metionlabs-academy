# spring-boot-dockers

Spring Boot 애플리케이션을 **도커 이미지로 만들고 컨테이너로 띄우는 것**까지를 다루는 예제입니다.

- Java 25 (LTS)
- Spring Boot 4.1.0
- Gradle 9.5.1 (Kotlin DSL — `build.gradle.kts`)
- Docker (multi-stage build)

## 이 예제로 배우는 것

1. **multi-stage build** — 빌드용 이미지(JDK)와 실행용 이미지(JRE)를 분리해서 최종 이미지를 작게 만드는 방법
2. **레이어 캐시** — 소스 한 줄 고쳤을 때 의존성을 다시 받지 않게 하는 `COPY` 순서
3. **컨테이너 안의 JVM** — 컨테이너에 걸린 메모리 제한을 JVM이 인식하게 만드는 옵션
4. **설정 주입** — 이미지는 그대로 두고 환경변수로 동작을 바꾸는 방법
5. **healthcheck** — "떠 있다"와 "요청 받을 준비가 됐다"의 차이

## 실행 방법

### 사전 준비

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) 설치 후 실행
- Java는 **설치 안 해도 됩니다.** 빌드가 컨테이너 안에서 일어나기 때문입니다.

터미널에서 아래가 나오면 준비 완료입니다.

```bash
docker --version
```

### 1. 컨테이너로 띄우기

```bash
docker compose up --build
```

처음 실행은 의존성을 받느라 몇 분 걸립니다. 두 번째부터는 캐시 덕분에 훨씬 빠릅니다.

브라우저에서 http://localhost:8080 접속하거나:

```bash
curl http://localhost:8080
```

```json
{
  "greeting": "Hello from Docker",
  "hostname": "e27ec270a75d",
  "javaVersion": "25.0.3+9-LTS",
  "uptime": "PT18.384S",
  "availableProcessors": 12,
  "maxMemoryMb": 371
}
```

각 값이 말해주는 것:

| 필드 | 확인 포인트 |
|------|------|
| `greeting` | `application.properties`의 기본값이 아니라 `compose.yaml`의 환경변수 값이 나온다 → **설정 주입이 동작함** |
| `hostname` | 노트북 이름이 아니라 컨테이너 ID → **격리된 환경에서 실행 중** |
| `javaVersion` | 내 노트북 JDK 버전과 무관하게 항상 25 → **실행 환경이 이미지에 고정됨** |
| `maxMemoryMb` | 512MB 제한의 약 75% → **JVM이 컨테이너 메모리 제한을 인식함** |

`hostname`이 노트북 이름이 아니라 **컨테이너 ID**로 나오는 것을 확인하세요.
지금 이 코드는 내 맥이 아니라 격리된 리눅스 컨테이너 안에서 돌고 있습니다.

### 2. 끄기

```bash
docker compose down
```

### 3. compose 없이 직접 해보기

compose가 대신 해주던 것을 손으로 하면 각 단계가 뭘 하는지 보입니다.

```bash
# 이미지 빌드
docker build -t spring-boot-dockers .

# 컨테이너 실행 (-p 호스트포트:컨테이너포트)
docker run --rm -p 8080:8080 spring-boot-dockers

# 환경변수를 바꿔서 실행
docker run --rm -p 8080:8080 -e ACADEMY_GREETING="안녕하세요" spring-boot-dockers
```

## 직접 해볼 것

### 1. 컨테이너는 각자 다른 개체다

```bash
docker compose up --build --scale app=3
```

포트 충돌이 납니다. `compose.yaml`의 `ports`를 `- "8080"`으로 바꾸고(호스트 포트를 도커가 알아서 배정) 다시 돌린 뒤,
`docker compose ps`로 배정된 포트를 확인해서 각각 찔러보세요. `hostname`이 전부 다르게 나옵니다.

### 2. 메모리 제한이 JVM에 먹히는지 확인

`compose.yaml`의 `memory: 512M`을 `256M`으로 바꾸고 다시 띄운 뒤 `maxMemoryMb`를 보세요.
제한값의 약 75%로 따라 내려갑니다 (`Dockerfile`의 `-XX:MaxRAMPercentage=75.0` 때문).

이 옵션을 지우면 어떻게 되는지도 해보세요. JVM이 컨테이너 제한을 무시하고 힙을 크게 잡다가
`docker stats`에서 메모리가 치솟고 결국 `OOMKilled`로 죽는 걸 볼 수 있습니다. 실무에서 자주 나는 사고입니다.

### 3. 레이어 캐시 체감하기

`HelloController.java`의 문구만 한 글자 바꾸고 다시 빌드해보세요.

```bash
time docker compose build
```

의존성을 다시 받지 않고 마지막 레이어만 새로 만들어져서 훨씬 빠릅니다.
이번엔 `build.gradle.kts`를 건드리고 다시 빌드해보세요. 의존성부터 다시 받습니다.
**`Dockerfile`에서 `COPY` 순서를 왜 그렇게 짰는지**가 여기서 드러납니다.

### 4. 이미지 크기 비교

```bash
docker images | grep spring-boot-dockers
```

`Dockerfile`의 런타임 단계를 `eclipse-temurin:25-jdk`로 바꿔서 다시 빌드하고 크기를 비교해보세요.
JRE만 쓸 때와 몇 백 MB 차이가 납니다.

## 자주 만나는 문제

| 증상 | 원인 / 해결 |
|------|------|
| `Cannot connect to the Docker daemon` | Docker Desktop이 안 떠 있습니다. 앱을 실행하고 고래 아이콘이 안정될 때까지 기다리세요. |
| `port is already allocated` | 8080을 이미 다른 게 쓰고 있습니다. `lsof -i :8080`으로 찾아서 끄거나, compose의 포트를 `"18080:8080"`으로 바꾸세요. |
| 빌드가 매번 느리다 | `.dockerignore`가 없으면 `build/`, `.git/`까지 통째로 전송됩니다. 이 예제엔 이미 들어 있습니다. |
| 소스를 고쳤는데 반영이 안 된다 | `docker compose up`만 하면 기존 이미지를 씁니다. `--build`를 붙이세요. |
