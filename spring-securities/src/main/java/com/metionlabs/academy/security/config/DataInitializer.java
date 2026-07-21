package com.metionlabs.academy.security.config;

import com.metionlabs.academy.security.domain.Role;
import com.metionlabs.academy.security.domain.User;
import com.metionlabs.academy.security.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기동 시 기본 계정을 만들어주는 클래스.
 *
 * <p>회원가입으로는 ADMIN 을 만들 수 없게 막아두었으므로(권한 상승 방지),
 * 관리자 계정은 이렇게 초기 데이터로만 넣는다. 인가(403) 실습을 하려면 ADMIN 이 하나는 있어야 한다.
 *
 * <p>{@code @Profile("!test")} — 테스트 때는 실행되지 않는다.
 * (테스트는 필요한 계정을 각자 직접 만든다)
 *
 * <p><b>[보안 주의] 여기 하드코딩된 비밀번호는 학습용이다.</b>
 * 실무에서 이렇게 코드에 관리자 비번을 박으면 안 된다. 최소한 환경변수로 주입하고,
 * 첫 로그인 시 비밀번호 변경을 강제하는 식으로 처리한다.
 */
@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * admin / user 기본 계정을 만든다. 이미 있으면 건너뛴다.
	 *
	 * @param args 사용하지 않음
	 */
	@Override
	@Transactional
	public void run(String... args) {
		if (userRepository.count() > 0) {
			log.info("사용자가 이미 있습니다. 초기 계정 생성을 건너뜁니다.");
			return;
		}

		// 관리자 계정. ADMIN 권한.
		userRepository.save(new User(
				"admin",
				passwordEncoder.encode("admin1234"), // 반드시 해시해서 저장
				Role.ADMIN));

		// 일반 사용자 계정. USER 권한.
		userRepository.save(new User(
				"user",
				passwordEncoder.encode("user1234"),
				Role.USER));

		log.info("초기 계정 생성 완료: admin(ADMIN) / user(USER)");
	}

}
