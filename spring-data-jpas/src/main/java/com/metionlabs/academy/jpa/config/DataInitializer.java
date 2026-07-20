package com.metionlabs.academy.jpa.config;

import java.time.LocalDate;

import com.metionlabs.academy.jpa.domain.Author;
import com.metionlabs.academy.jpa.domain.Book;
import com.metionlabs.academy.jpa.repository.AuthorRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 애플리케이션이 시작될 때 샘플 데이터를 넣어주는 클래스.
 *
 * <p>데이터가 하나도 없으면 N+1 실습을 할 수가 없어서 만들었다.
 * 학생이 직접 POST 로 데이터를 여러 건 넣는 수고를 덜어준다.
 *
 * <p><b>{@code CommandLineRunner}</b> = 스프링 부트가 <b>기동을 마친 직후</b>
 * {@code run()} 을 한 번 호출해준다. 초기 데이터 세팅, 캐시 예열 등에 쓴다.
 *
 * <p><b>{@code @Profile("!test")} 의 의미:</b>
 * {@code !} 는 부정이다. "test 프로파일이 아닐 때만 이 빈을 등록하라".
 * 테스트를 돌릴 때는 샘플 데이터가 들어가면 안 되기 때문이다.
 * 테스트는 자기가 필요한 데이터를 직접 준비해야 결과가 예측 가능해진다.
 * (샘플 데이터에 의존하면, 샘플을 한 줄 바꿨을 뿐인데 무관한 테스트가 깨진다)
 */
@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

	// 로거는 보통 static final 로 선언한다.
	// 인스턴스마다 만들 이유가 없고, 클래스 정보를 넘겨 로그에 출처가 찍히게 한다.
	private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

	private final AuthorRepository authorRepository;

	public DataInitializer(AuthorRepository authorRepository) {
		this.authorRepository = authorRepository;
	}

	/**
	 * 샘플 데이터를 넣는다. 이미 데이터가 있으면 아무것도 하지 않는다.
	 *
	 * <p>{@code @Transactional} 이 필요한 이유: {@code author.addBook()} 으로 추가한 책들이
	 * cascade 로 함께 저장되려면 하나의 트랜잭션 안에서 처리돼야 한다.
	 *
	 * @param args 커맨드라인 인자 (사용하지 않음)
	 */
	@Override
	@Transactional
	public void run(String... args) {
		// 이미 데이터가 있으면 건너뛴다.
		// 이 검사가 없으면 컨테이너를 재시작할 때마다 같은 데이터가 계속 쌓인다.
		// (compose 의 볼륨 덕분에 DB 데이터는 재시작해도 남아 있기 때문)
		if (authorRepository.count() > 0) {
			log.info("샘플 데이터가 이미 있습니다. 초기화를 건너뜁니다. (저자 {}명)", authorRepository.count());
			return;
		}

		log.info("샘플 데이터를 넣습니다...");

		// 저자 3명 + 각자의 책들.
		// N+1 을 눈으로 보려면 저자가 최소 2~3명은 있어야 한다.
		// (1명이면 추가 쿼리도 1번이라 "반복" 이 안 보인다)

		Author kim = new Author("김영한", "kim@example.com");
		kim.addBook(new Book("자바 ORM 표준 JPA 프로그래밍", 43000, LocalDate.of(2015, 7, 27)));
		kim.addBook(new Book("스프링 핵심 원리", 38000, LocalDate.of(2021, 3, 15)));

		Author park = new Author("박병주", "park@example.com");
		park.addBook(new Book("도커로 시작하는 백엔드", 32000, LocalDate.of(2026, 1, 10)));
		park.addBook(new Book("실전 Spring Data JPA", 35000, LocalDate.of(2026, 5, 2)));
		park.addBook(new Book("멀티테넌트 서비스 설계", 41000, LocalDate.of(2026, 6, 20)));

		// 책이 한 권도 없는 저자.
		// LEFT JOIN 과 INNER JOIN 의 차이를 확인하는 용도로 일부러 넣었다.
		// findAllWithBooks() 의 LEFT JOIN 을 그냥 JOIN 으로 바꿔보면 이 저자가 사라진다.
		Author lee = new Author("이신입", "lee@example.com");

		// saveAll = 여러 건을 한 번에 저장. 내부적으로 save 를 반복하지만 코드가 간결해진다.
		authorRepository.saveAll(java.util.List.of(kim, park, lee));

		log.info("샘플 데이터 준비 완료: 저자 {}명", authorRepository.count());
	}

}
