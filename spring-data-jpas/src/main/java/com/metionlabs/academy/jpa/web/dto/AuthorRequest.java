package com.metionlabs.academy.jpa.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 저자 등록 요청 DTO.
 *
 * <p><b>DTO(Data Transfer Object)란?</b> 계층 간에 데이터를 옮기기 위한 객체다.
 * 여기서는 "HTTP 요청 본문(JSON)" 을 자바 객체로 받는 역할을 한다.
 *
 * <p><b>왜 엔티티({@code Author})를 그대로 요청 타입으로 쓰지 않는가?</b>
 * <ol>
 *   <li><b>보안</b> — 엔티티를 그대로 받으면 클라이언트가 {@code id} 같은
 *       건드리면 안 되는 필드까지 값을 넣어 보낼 수 있다.
 *       (다른 사람의 데이터를 덮어쓰는 공격이 가능해진다)</li>
 *   <li><b>결합도</b> — DB 컬럼을 하나 바꾸면 API 스펙이 같이 바뀌어버린다.
 *       DTO 를 두면 내부 구조를 바꿔도 외부 API 는 그대로 유지할 수 있다.</li>
 *   <li><b>검증 위치</b> — 입력 검증 규칙은 API 의 관심사지 DB 테이블의 관심사가 아니다.</li>
 * </ol>
 *
 * <p><b>{@code record} 를 쓴 이유:</b>
 * 자바 16부터 정식 도입된 문법으로, 값을 담기만 하는 클래스를 한 줄로 만든다.
 * 생성자, getter, {@code equals()}, {@code hashCode()}, {@code toString()} 이 자동 생성되고
 * 모든 필드가 {@code final} 이라 만들어진 뒤에는 값이 바뀌지 않는다.
 * DTO 는 "받은 값을 그대로 전달만" 하면 되므로 record 가 딱 맞는다.
 *
 * <p>참고: record 는 <b>엔티티로는 쓸 수 없다.</b>
 * JPA 는 기본 생성자와 필드 변경이 가능해야 하는데 record 는 둘 다 불가능하기 때문이다.
 *
 * @param name  저자 이름
 * @param email 이메일
 */
public record AuthorRequest(

		// @NotBlank = null 도 아니고, 빈 문자열도 아니고, 공백만 있는 것도 아니어야 한다.
		//   비슷한 것들과 구분:
		//     @NotNull  : null 만 막는다. "" 나 "   " 는 통과한다.
		//     @NotEmpty : null 과 "" 를 막는다. "   " 는 통과한다.
		//     @NotBlank : 셋 다 막는다. 문자열 검증은 대개 이게 맞다.
		//
		// message 를 지정하지 않으면 영어 기본 메시지가 나간다.
		// 사용자에게 그대로 보일 수 있으니 한국어로 적어준다.
		@NotBlank(message = "저자 이름은 필수입니다")

		// 엔티티의 @Column(length = 50) 과 숫자를 맞춰야 한다.
		// 여기서 막지 않으면 DB 까지 갔다가 길이 초과 예외가 터지는데,
		// 그건 500 에러라서 "서버 잘못" 처럼 보인다.
		// 여기서 걸러야 400(잘못된 요청)으로 친절하게 돌려줄 수 있다.
		@Size(max = 50, message = "저자 이름은 50자를 넘을 수 없습니다")
		String name,

		@NotBlank(message = "이메일은 필수입니다")
		// @Email = 이메일 형식인지 검사. 다만 형식만 볼 뿐 실제 존재 여부는 모른다.
		@Email(message = "이메일 형식이 올바르지 않습니다")
		@Size(max = 100, message = "이메일은 100자를 넘을 수 없습니다")
		String email

) {
	// ------------------------------------------------------------------------
	// [중요] 위 검증 애노테이션들은 그냥 붙여둔다고 동작하지 않는다.
	// 컨트롤러의 파라미터에 @Valid 를 붙여야 실제로 검사가 실행된다.
	//   public ... create(@Valid @RequestBody AuthorRequest request)
	//                     ^^^^^^ 이게 없으면 검증이 통째로 무시된다 (조용히!)
	// ------------------------------------------------------------------------
}
