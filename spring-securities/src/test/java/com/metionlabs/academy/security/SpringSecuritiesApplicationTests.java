package com.metionlabs.academy.security;

import com.jayway.jsonpath.JsonPath;
import com.metionlabs.academy.security.domain.Role;
import com.metionlabs.academy.security.domain.User;
import com.metionlabs.academy.security.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인증·인가 전체 흐름 테스트.
 *
 * <p>진짜 MySQL(Testcontainers) 위에서 애플리케이션을 통째로 띄우고,
 * 실제 HTTP 요청을 흉내내(MockMvc) 회원가입 → 로그인 → 보호된 API 접근까지 검증한다.
 *
 * <p><b>이 테스트가 곧 이 예제의 명세서 역할을 한다.</b>
 * "토큰 없으면 401, 권한 부족하면 403, 올바른 토큰이면 200" 이 코드로 박제되어 있어서,
 * 나중에 누가 SecurityConfig 를 잘못 고치면 여기서 바로 빨간불이 켜진다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc // MockMvc 로 컨트롤러에 가짜 HTTP 요청을 넣을 수 있게 한다
@ActiveProfiles("test") // DataInitializer(@Profile("!test"))가 안 돌아 초기 계정이 안 생긴다 → 테스트가 직접 준비
class SpringSecuritiesApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		// 각 테스트가 깨끗한 상태에서 시작하도록 비운다.
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("회원가입하면 201과 사용자 정보가 반환되고, 응답에 비밀번호는 없다")
	void signup() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"hong","password":"password123"}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("hong"))
				.andExpect(jsonPath("$.role").value("USER"))
				// [보안 검증] 응답에 password 필드가 존재하지 않아야 한다.
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	@DisplayName("비밀번호는 평문이 아니라 해시로 저장된다")
	void passwordIsHashed() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"hong","password":"password123"}"""))
				.andExpect(status().isCreated());

		User saved = userRepository.findByUsername("hong").orElseThrow();
		// 저장된 값이 평문과 같으면 안 된다.
		assertThat(saved.getPassword()).isNotEqualTo("password123");
		// 그리고 인코더로 대조하면 일치해야 한다(= 올바른 해시).
		assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
	}

	@Test
	@DisplayName("짧은 비밀번호로 회원가입하면 400과 필드 메시지가 반환된다")
	void signupValidationFails() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"hong","password":"short"}""")) // 8자 미만
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.password").exists());
	}

	@Test
	@DisplayName("로그인하면 JWT 토큰이 발급된다")
	void login() throws Exception {
		signupUser("hong", "password123", Role.USER);

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"hong","password":"password123"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists())
				.andExpect(jsonPath("$.tokenType").value("Bearer"));
	}

	@Test
	@DisplayName("틀린 비밀번호로 로그인하면 401이 반환된다")
	void loginWrongPassword() throws Exception {
		signupUser("hong", "password123", Role.USER);

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"hong","password":"wrongpassword"}"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("토큰 없이 보호된 API를 호출하면 401이 반환된다")
	void protectedWithoutToken() throws Exception {
		mockMvc.perform(get("/api/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("유효한 토큰으로 /api/me를 호출하면 내 정보가 반환된다")
	void meWithToken() throws Exception {
		signupUser("hong", "password123", Role.USER);
		String token = loginAndGetToken("hong", "password123");

		mockMvc.perform(get("/api/me")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("hong"));
	}

	@Test
	@DisplayName("USER 권한으로 관리자 API를 호출하면 403이 반환된다")
	void adminForbiddenForUser() throws Exception {
		signupUser("hong", "password123", Role.USER);
		String token = loginAndGetToken("hong", "password123");

		// 인증은 됐지만(누구인지 앎) 권한이 없다(USER) → 403 (401 이 아님에 주목)
		mockMvc.perform(get("/api/admin/users")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("ADMIN 권한으로 관리자 API를 호출하면 200이 반환된다")
	void adminAllowedForAdmin() throws Exception {
		signupUser("boss", "password123", Role.ADMIN);
		String token = loginAndGetToken("boss", "password123");

		mockMvc.perform(get("/api/admin/users")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("위조된(엉터리) 토큰으로 호출하면 401이 반환된다")
	void forgedTokenRejected() throws Exception {
		mockMvc.perform(get("/api/me")
						.header("Authorization", "Bearer this.is.not-a-valid-jwt"))
				.andExpect(status().isUnauthorized());
	}

	// ------------------------------------------------------------------------
	// 테스트 헬퍼
	// ------------------------------------------------------------------------

	/**
	 * 지정한 권한으로 사용자를 직접 저장한다.
	 *
	 * <p>회원가입 API 는 항상 USER 로만 만들 수 있어서(권한 상승 방지),
	 * ADMIN 계정이 필요한 테스트는 리포지토리로 직접 넣는다.
	 */
	private void signupUser(String username, String rawPassword, Role role) {
		userRepository.save(new User(username, passwordEncoder.encode(rawPassword), role));
	}

	/**
	 * 로그인해서 accessToken 문자열만 뽑아낸다.
	 */
	private String loginAndGetToken(String username, String rawPassword) throws Exception {
		String json = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"%s","password":"%s"}""".formatted(username, rawPassword)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		// JsonPath 는 MockMvc 의 jsonPath(...)가 내부에서 쓰는 라이브러리라 test 클래스패스에 이미 있다.
		// (Jackson ObjectMapper 는 Boot 4 에서 test 컴파일 클래스패스에 바로 노출되지 않아 이걸 쓴다)
		return JsonPath.read(json, "$.accessToken");
	}

}
