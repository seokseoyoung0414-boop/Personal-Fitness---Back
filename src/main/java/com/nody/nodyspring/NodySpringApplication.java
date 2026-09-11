package com.nody.nodyspring;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class NodySpringApplication {

    public static void main(String[] args) {
        // 1. .env 파일 로드 (파일이 없어도 에러 없이 무시하도록 ignoreIfMissing 설정)
        Dotenv dotenv = Dotenv.configure()
                .filename("env.txt")
                .ignoreIfMissing()
                .load();
        // 2. .env의 모든 Key-Value를 System.setProperty로 등록
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });

        // 3. Spring Boot 실행 (이제 ${...} 구문에서 .env 값을 정상 인식함)
        SpringApplication.run(NodySpringApplication.class, args);
    }

    @PostConstruct
    public void setTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        System.out.println("✅ 서버 시간대 설정됨: " + TimeZone.getDefault());
    }
}