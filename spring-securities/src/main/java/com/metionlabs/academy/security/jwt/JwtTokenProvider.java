package com.metionlabs.academy.security.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import com.metionlabs.academy.security.domain.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰을 <b>만들고</b>(로그인 성공 시) <b>검증한다</b>(요청이 올 때마다).
 *
 * <p>=========================================================================
 * <p><b>JWT 란 무엇인가?</b>
 * <p>=========================================================================
 * JWT(JSON Web Token)는 점(.)으로 구분된 세 부분으로 된 문자열이다.
 * <pre>
 *   eyJhbGciOi...  .  eyJzdWIiOi...  .  SflKxwRJSM...
 *   └─ 헤더 ─┘      └─ 페이로드 ─┘     └─ 서명 ─┘
 *
 *   헤더(header)     : 어떤 알고리즘으로 서명했는지 (예: HS256)
 *   페이로드(payload): 담긴 정보 (누구인지=sub, 권한=role, 만료시각=exp 등)
 *   서명(signature)  : 위 둘을 secret 키로 서명한 값
 * </pre>
 *
 * <p><b>[매우 중요] 헤더와 페이로드는 암호화가 아니라 그냥 Base64 인코딩이다.</b>
 * 즉 <b>누구나 열어볼 수 있다.</b> (jwt.io 에 붙여넣으면 내용이 다 보인다)
 * 그래서 비밀번호, 주민번호 같은 <b>민감 정보를 JWT 에 넣으면 절대 안 된다.</b>
 *
 * <p>그럼 뭘 믿는가? <b>서명</b>이다. 페이로드를 조금이라도 고치면 서명이 안 맞게 되고,
 * 서명을 다시 만들려면 secret 키가 필요한데 서버만 그 키를 안다.
 * 그래서 "내용은 공개돼도, 위조는 불가능한" 성질을 갖는다.
 *
 * <p>=========================================================================
 * <p><b>왜 JWT 를 쓰는가? (세션과의 차이)</b>
 * <p>=========================================================================
 * <ul>
 *   <li><b>세션 방식</b> — 서버가 로그인 상태를 메모리/DB 에 저장하고,
 *       클라이언트에는 세션 ID 만 준다. 서버가 상태를 "기억" 한다(stateful).
 *       서버가 여러 대면 세션을 공유해야 해서 번거롭다.</li>
 *   <li><b>JWT 방식</b> — 서버는 아무것도 저장하지 않는다(stateless).
 *       토큰 안에 "나는 누구, 권한은 무엇" 이 다 들어 있고 서명으로 위조를 막는다.
 *       서버가 몇 대든 secret 키만 공유하면 되어 확장에 유리하다.
 *       SPA·모바일 앱이 JWT 를 즐겨 쓰는 이유다.</li>
 * </ul>
 *
 * <p><b>[학습용 구현임을 명시]</b> 이 예제는 필터와 토큰 발급을 직접 짜서 원리를 배운다.
 * 실무에서는 Spring Security 의 {@code oauth2-resource-server}(검증)나
 * Keycloak·Auth0 같은 전용 인증 서버(IdP)를 쓰기도 한다.
 * 또한 실무 JWT 는 보통 access token(짧게) + refresh token(길게) 두 개를 쓰는데,
 * 이 예제는 개념 집중을 위해 access token 하나만 다룬다.
 */
@Component
public class JwtTokenProvider {

	private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

	/** 페이로드에 권한을 담을 때 쓸 키 이름. (표준 필드가 아닌 커스텀 클레임) */
	private static final String ROLE_CLAIM = "role";

	private final SecretKey key;
	private final long expirationMillis;

	/**
	 * @param properties JWT 설정값 (secret, 만료시간)
	 */
	public JwtTokenProvider(JwtProperties properties) {
		// 문자열 secret 을 서명용 키 객체로 변환한다.
		//
		// [보안] HS256 알고리즘은 최소 256비트(=32바이트) 키를 요구한다.
		// secret 문자열이 32자보다 짧으면 여기서 예외가 나며 애플리케이션이 안 뜬다.
		// 오히려 다행이다 — 약한 키로 서비스가 뜨는 것보다, 시작할 때 막히는 게 낫다.
		this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
		this.expirationMillis = properties.expirationMillis();
	}

	/**
	 * 로그인에 성공한 사용자에게 줄 토큰을 만든다.
	 *
	 * @param username 사용자 아이디
	 * @param role     사용자 권한
	 * @return 서명된 JWT 문자열
	 */
	public String createToken(String username, Role role) {
		Instant now = Instant.now();
		Instant expiry = now.plusMillis(expirationMillis);

		return Jwts.builder()
				// sub(subject) = 이 토큰의 주인. 보통 사용자 식별자를 넣는다.
				.subject(username)
				// 커스텀 클레임으로 권한을 담는다. 검증할 때 이걸 꺼내 인가에 쓴다.
				// [주의] 권한을 토큰에 담으면, 토큰 발급 후 서버에서 권한을 바꿔도
				//        기존 토큰은 옛 권한을 그대로 들고 다닌다(만료 전까지).
				//        그래서 만료 시간을 너무 길게 잡으면 안 된다.
				.claim(ROLE_CLAIM, role.name())
				// iat(issued at) = 발급 시각
				.issuedAt(Date.from(now))
				// exp(expiration) = 만료 시각. 이 시각이 지나면 검증이 실패한다.
				.expiration(Date.from(expiry))
				// secret 키로 서명한다. 이 줄이 위조를 막는 핵심이다.
				.signWith(key)
				// 최종 문자열로 직렬화한다.
				.compact();
	}

	/**
	 * 토큰이 유효한지 검사한다. (서명이 맞는지 + 만료되지 않았는지)
	 *
	 * @param token 검사할 JWT 문자열
	 * @return 유효하면 true
	 */
	public boolean validate(String token) {
		try {
			parse(token);
			return true;
		}
		catch (ExpiredJwtException ex) {
			// 만료는 "정상적인" 실패다. 토큰은 원래 시간이 지나면 만료된다.
			// 그래서 에러가 아니라 debug 로만 남긴다. (error 로 남기면 로그가 시끄러워진다)
			log.debug("만료된 토큰입니다: {}", ex.getMessage());
			return false;
		}
		catch (JwtException | IllegalArgumentException ex) {
			// 서명이 안 맞거나, 형식이 깨졌거나, 위조된 토큰.
			// 이건 정상이 아니므로 조금 더 눈에 띄게 남긴다.
			log.warn("유효하지 않은 토큰입니다: {}", ex.getMessage());
			return false;
		}
	}

	/**
	 * 토큰에서 사용자 아이디(subject)를 꺼낸다.
	 *
	 * <p>{@link #validate(String)} 로 유효성을 확인한 뒤에 호출해야 한다.
	 *
	 * @param token JWT 문자열
	 * @return 사용자 아이디
	 */
	public String getUsername(String token) {
		return parse(token).getSubject();
	}

	/**
	 * 토큰에서 권한을 꺼낸다.
	 *
	 * @param token JWT 문자열
	 * @return 권한
	 */
	public Role getRole(String token) {
		String role = parse(token).get(ROLE_CLAIM, String.class);
		return Role.valueOf(role);
	}

	/**
	 * 토큰을 파싱하고 서명·만료를 함께 검증한다.
	 *
	 * <p>파싱 과정에서 서명이 안 맞거나 만료됐으면 예외가 발생한다.
	 * 즉 이 메서드가 성공적으로 리턴하면 "검증까지 통과한" 것이다.
	 *
	 * @param token JWT 문자열
	 * @return 페이로드(클레임 모음)
	 */
	private Claims parse(String token) {
		return Jwts.parser()
				// 검증에 쓸 키를 지정한다. 발급할 때와 같은 키여야 서명이 맞는다.
				.verifyWith(key)
				.build()
				// 서명·만료를 검증하면서 파싱한다. 문제가 있으면 여기서 예외를 던진다.
				.parseSignedClaims(token)
				.getPayload();
	}

}
