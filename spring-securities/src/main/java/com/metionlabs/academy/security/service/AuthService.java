package com.metionlabs.academy.security.service;

import com.metionlabs.academy.security.domain.Role;
import com.metionlabs.academy.security.domain.User;
import com.metionlabs.academy.security.jwt.JwtTokenProvider;
import com.metionlabs.academy.security.repository.UserRepository;
import com.metionlabs.academy.security.web.dto.LoginRequest;
import com.metionlabs.academy.security.web.dto.SignupRequest;
import com.metionlabs.academy.security.web.dto.TokenResponse;
import com.metionlabs.academy.security.web.dto.UserResponse;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입·로그인 업무 로직.
 *
 * <p>인증의 두 흐름을 담당한다.
 * <ul>
 *   <li><b>회원가입</b> — 비밀번호를 해시해서 사용자를 저장한다</li>
 *   <li><b>로그인</b> — 아이디/비번을 대조하고, 맞으면 JWT 토큰을 발급한다</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true) // 기본은 읽기 전용, 데이터를 바꾸는 signup 에만 @Transactional 재선언
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtTokenProvider jwtTokenProvider) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	/**
	 * 회원가입.
	 *
	 * @param request 가입 요청 (평문 비밀번호 포함)
	 * @return 가입된 사용자 정보 (비밀번호 제외)
	 * @throws DuplicateUsernameException 아이디가 이미 존재할 때
	 */
	@Transactional
	public UserResponse signup(SignupRequest request) {
		// 아이디 중복 검사. (최종 방어선은 DB 의 unique 제약)
		if (userRepository.existsByUsername(request.username())) {
			throw new DuplicateUsernameException(request.username());
		}

		// [핵심] 비밀번호를 여기서 해시한다.
		// 평문 request.password() 를 절대 그대로 저장하지 않는다.
		// encode() 는 salt 를 포함한 BCrypt 해시(60자)를 만들어준다.
		String hashed = passwordEncoder.encode(request.password());

		// 신규 가입자는 일반 사용자(USER) 권한으로 시작한다.
		// ADMIN 을 회원가입으로 받으면, 누구나 관리자가 될 수 있어 위험하다.
		// 관리자 승격은 별도의 관리자 전용 기능으로만 해야 한다.
		User user = new User(request.username(), hashed, Role.USER);

		User saved = userRepository.save(user);
		return UserResponse.from(saved);
	}

	/**
	 * 로그인. 성공하면 JWT 토큰을 발급한다.
	 *
	 * @param request 로그인 요청
	 * @return 발급된 토큰
	 * @throws InvalidCredentialsException 아이디가 없거나 비밀번호가 틀렸을 때
	 */
	public TokenResponse login(LoginRequest request) {
		// 아이디로 사용자를 찾는다.
		User user = userRepository.findByUsername(request.username())
				// [보안] "아이디가 없음" 과 "비번이 틀림" 을 구분해서 알려주지 않는다.
				// 둘 다 같은 예외로 처리해서, 공격자가 "이 아이디는 존재한다" 를
				// 알아내지 못하게 한다(사용자 열거 공격 방지).
				.orElseThrow(InvalidCredentialsException::new);

		// [핵심] 비밀번호 대조.
		// 저장된 해시를 "복호화" 하는 게 아니다(해시는 되돌릴 수 없다).
		// 입력 평문을 같은 방식으로 해시해서, 저장된 해시와 같은지 matches() 로 비교한다.
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new InvalidCredentialsException();
		}

		// 인증 성공 → 토큰 발급.
		// 이 토큰 안에 username 과 role 이 서명된 채로 담긴다.
		String token = jwtTokenProvider.createToken(user.getUsername(), user.getRole());
		return TokenResponse.bearer(token);
	}

}
