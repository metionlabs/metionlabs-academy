package com.metionlabs.academy.security.config;

import java.util.List;

import com.metionlabs.academy.security.domain.User;
import com.metionlabs.academy.security.repository.UserRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * "아이디로 사용자를 찾아 Spring Security 에게 넘겨주는" 다리 역할.
 *
 * <p><b>왜 이게 필요한가?</b>
 * Spring Security 는 우리 {@code User} 엔티티를 모른다.
 * 대신 {@code UserDetails} 라는 자기만의 인터페이스로 사용자를 다룬다.
 * 그래서 "우리 User -> 시큐리티의 UserDetails" 로 번역해주는 코드가 필요하고,
 * 그 진입점이 {@code UserDetailsService} 인터페이스의 {@code loadUserByUsername} 메서드다.
 *
 * <p>이 예제에서는 로그인 시 이 서비스로 사용자를 찾아 비밀번호를 대조한다.
 * (JWT 필터에서도 토큰의 주인을 다시 확인하는 데 쓸 수 있다)
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * 아이디로 사용자를 찾아 {@link UserDetails} 로 변환한다.
	 *
	 * <p><b>[보안 주의] 사용자를 못 찾아도 "아이디가 없다" 고 구체적으로 알려주면 안 된다.</b>
	 * 공격자가 그 정보로 "어떤 아이디가 존재하는지" 를 알아낼 수 있기 때문이다(사용자 열거 공격).
	 * 그래서 로그인 실패 응답은 아이디가 틀렸든 비번이 틀렸든 똑같이 "인증 실패" 로 준다.
	 * (여기서는 프레임워크 규약상 예외를 던지지만, 최종 응답 메시지는 뭉뚱그린다)
	 *
	 * @param username 로그인 아이디
	 * @return 시큐리티가 쓰는 사용자 정보
	 * @throws UsernameNotFoundException 사용자가 없을 때
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("인증 실패"));

		// org.springframework.security.core.userdetails.User 는 시큐리티가 기본 제공하는
		// UserDetails 구현체다. (우리 도메인의 User 와 이름만 같고 다른 클래스다 — 헷갈리지 말 것)
		//
		// 여기서 권한에 role.toAuthority() 로 "ROLE_" 접두사를 붙인다.
		// 이게 있어야 나중에 hasRole("ADMIN") 검사가 동작한다.
		return org.springframework.security.core.userdetails.User.builder()
				.username(user.getUsername())
				.password(user.getPassword()) // 이미 해시된 값. 시큐리티가 대조 시 인코더로 비교한다.
				.authorities(List.of(new SimpleGrantedAuthority(user.getRole().toAuthority())))
				.build();
	}

}
