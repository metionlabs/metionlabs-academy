package com.metionlabs.academy.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 사용자 엔티티.
 *
 * <p><b>[가장 중요] 비밀번호는 절대 평문으로 저장하지 않는다.</b>
 * 이 필드에 들어가는 값은 원문이 아니라 <b>BCrypt 로 해시된 값</b>이다.
 * (해시 = 원문으로 되돌릴 수 없는 단방향 변환)
 *
 * <p>왜? DB 가 유출되는 사고는 언제든 일어난다.
 * 평문으로 저장했다면 그 순간 모든 사용자의 비밀번호가 그대로 털린다.
 * 사람들은 같은 비밀번호를 여러 사이트에 쓰기 때문에, 피해가 우리 서비스에서 끝나지 않는다.
 * 해시로 저장했다면 유출돼도 원문을 알아내기가 매우 어렵다.
 *
 * <p>해시는 서비스 계층({@code AuthService})에서 {@code PasswordEncoder} 로 처리한다.
 * 이 엔티티는 "이미 해시된 값이 들어온다" 고 가정한다.
 *
 * <p><b>{@code java.util.Date} 를 왜 안 쓰는지, {@code Long} vs {@code long} 등
 * JPA 매핑 기본기는 spring-data-jpas 예제의 Author/Book 주석에 자세히 있다.</b>
 */
@Entity
@Table(name = "users")
// 테이블명을 굳이 명시한 이유: user 는 여러 DB(특히 PostgreSQL, Oracle)에서 예약어라
// 그대로 쓰면 SQL 문법 오류가 난다. users 로 짓는 게 안전하다.
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 로그인 아이디. 중복될 수 없다.
	 *
	 * <p>{@code unique = true} 로 DB 가 중복을 막는다.
	 * 애플리케이션에서 "이미 있는지 조회 후 저장" 만으로는 동시 요청 시 뚫릴 수 있어서,
	 * 최종 방어선으로 DB 제약을 둔다.
	 */
	@Column(nullable = false, unique = true, length = 50)
	private String username;

	/**
	 * BCrypt 로 해시된 비밀번호.
	 *
	 * <p>길이를 넉넉히 잡은 이유: BCrypt 해시 결과는 항상 60자다.
	 * 다른 알고리즘으로 바꿀 여지까지 두려고 100 으로 잡았다.
	 * 평문 길이를 기준으로 잡으면(예: 20) 해시가 잘려서 인증이 깨진다.
	 */
	@Column(nullable = false, length = 100)
	private String password;

	/**
	 * 권한.
	 *
	 * <p><b>{@code @Enumerated(EnumType.STRING)} 이 매우 중요하다.</b>
	 * enum 을 DB 에 저장하는 방식은 두 가지다.
	 * <ul>
	 *   <li>{@code ORDINAL} (기본값) — enum 의 순서를 숫자로 저장한다. USER=0, ADMIN=1.
	 *       <b>이걸 쓰면 안 된다.</b> 나중에 enum 중간에 값을 추가하면
	 *       (예: USER, <u>MANAGER</u>, ADMIN) 순서가 밀려서,
	 *       이미 저장된 데이터의 의미가 통째로 바뀐다. 1번이 ADMIN 이었는데 MANAGER 가 된다.
	 *       조용히 일어나는 데다 권한 관련이라 보안 사고로 직결된다.</li>
	 *   <li>{@code STRING} — 이름("USER", "ADMIN")을 그대로 저장한다.
	 *       enum 순서를 바꿔도 안전하다. <b>enum 저장은 거의 항상 STRING 을 쓴다.</b></li>
	 * </ul>
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	/**
	 * JPA 용 기본 생성자. (protected 로 외부 무분별 생성 차단)
	 */
	protected User() {
	}

	/**
	 * @param username 로그인 아이디
	 * @param password <b>이미 해시된</b> 비밀번호 (평문을 넣으면 안 된다)
	 * @param role     권한
	 */
	public User(String username, String password, Role role) {
		this.username = username;
		this.password = password;
		this.role = role;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public Role getRole() {
		return role;
	}

}
