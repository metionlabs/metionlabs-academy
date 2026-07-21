package com.metionlabs.academy.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 관련 설정값을 담는 클래스.
 *
 * <p>{@code @ConfigurationProperties} = application.properties 의
 * {@code jwt.*} 로 시작하는 값들을 이 객체에 자동으로 채워준다.
 * <pre>
 *   jwt.secret=...        ->  secret
 *   jwt.expiration-millis=...  ->  expirationMillis  (케밥케이스 -> 카멜케이스 자동 변환)
 * </pre>
 *
 * <p><b>왜 값을 코드에 안 박고 설정으로 빼는가?</b>
 * secret(서명 키)은 환경마다 달라야 하고, 특히 <b>코드에 넣어 git 에 올리면
 * 그 순간 유출</b>이다. 설정으로 빼두면 환경변수(JWT_SECRET)로 주입할 수 있다.
 * compose.yaml 에서 그렇게 넘긴다.
 *
 * <p>{@code record} 로 만들면 값이 불변이라 안전하다.
 *
 * @param secret           토큰 서명에 쓰는 비밀 키. 유출되면 누구나 위조 토큰을 만들 수 있다.
 * @param expirationMillis 토큰 유효 시간(밀리초). 지나면 만료된다.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long expirationMillis) {
}
