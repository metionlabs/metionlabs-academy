package com.metionlabs.academy.security.repository;

import java.util.Optional;

import com.metionlabs.academy.security.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사용자 저장소.
 *
 * <p>JpaRepository 를 상속하면 save/findById/findAll 등이 공짜로 생긴다.
 * (자세한 원리는 spring-data-jpas 예제 참고)
 */
public interface UserRepository extends JpaRepository<User, Long> {

	/**
	 * 아이디로 사용자를 찾는다.
	 *
	 * <p>인증의 출발점이다. 로그인할 때, 그리고 JWT 토큰을 검증한 뒤
	 * "이 토큰의 주인이 실제로 존재하는지" 확인할 때 쓴다.
	 *
	 * <p>메서드 이름 {@code findByUsername} 이 곧
	 * {@code SELECT * FROM users WHERE username = ?} 가 된다.
	 *
	 * @param username 로그인 아이디
	 * @return 사용자 (없으면 빈 Optional)
	 */
	Optional<User> findByUsername(String username);

	/**
	 * 해당 아이디가 이미 쓰이고 있는지 확인한다.
	 *
	 * <p>회원가입 시 중복 검사에 쓴다.
	 * 사용자 객체 전체가 필요 없고 존재 여부만 알면 되므로,
	 * {@code findByUsername(...).isPresent()} 보다 {@code existsByUsername} 이 더 낫다.
	 * ({@code SELECT 1 ... LIMIT 1} 형태라 데이터를 실제로 읽어오지 않아 가볍다)
	 *
	 * @param username 로그인 아이디
	 * @return 존재하면 true
	 */
	boolean existsByUsername(String username);

}
