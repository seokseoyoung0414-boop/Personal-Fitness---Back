# **FitUpBackend(SpringBoot로 구성)**

### **환경설정**

- env.txt에 있는 내용을 기반으로 본인의 환경에 맞게 .env 작성

### **폴더 구조**

```
FitUpBackend/src/main/java/com/nody/nodyspring
├── config/         # 애플리케이션 설정, Bean 등록, 보안 설정 등
├── controller/     # 클라이언트 요청을 처리하고 서비스 호출 후 응답 반환
├── DB/             # 데이터베이스 관련 모듈
│   └── MongoDB/    # MongoDB 연동 모델
├── dto/            # 데이터 전송 객체 정의 (DTO)
├── repository/     # Repository 인터페이스 정의
├── service/        # 비즈니스 로직 처리
├── scheduler/      # 주기적 작업(cron, 배치) 스케줄러 정의
├── util/           # 공통 유틸리티 클래스 및 함수
└── main.java       # Spring Boot 애플리케이션 진입점
```
