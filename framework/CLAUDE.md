# Spring Boot Common Framework - CLAUDE.md

## 프로젝트 개요

Spring Boot 기반 공통 프레임워크 템플릿.
새 프로젝트 시작 시 이 템플릿을 기반으로 비즈니스 코드를 추가한다.

---

## 기술 스택

- **Java**: 17
- **Spring Boot**: 3.5.15
- **Build**: Gradle Groovy
- **DB**: PostgreSQL
- **ORM**: MyBatis
- **Security**: Spring Security + JWT (jjwt 0.12.6)
- **HTTP Client**: WebClient (WebFlux)

---

## 패키지 구조

```
src/main/java/com/framework/
└── common/
    ├── config/
    │   ├── SecurityConfig.java       # Spring Security + JWT 필터 등록
    │   ├── JwtFilter.java            # Bearer 토큰 추출 및 인증
    │   ├── WebMvcConfig.java         # CORS 설정
    │   └── WebClientConfig.java      # Python FastAPI 호출용 WebClient
    ├── exception/
    │   ├── ErrorCode.java            # 에러 코드 enum
    │   ├── BusinessException.java    # 커스텀 런타임 예외
    │   └── GlobalExceptionHandler.java # 전역 예외 처리
    ├── response/
    │   └── ApiResponse.java          # 공통 응답 래퍼
    └── util/
        ├── JwtProvider.java          # JWT 생성 및 검증
        ├── IdGenerator.java          # 공통 채번 유틸
        └── IdGeneratorMapper.java    # fn_generate_key() 호출 Mapper
```

---

## 공통 응답 포맷

### 항상 ApiResponse<T>로 반환한다

```java
// 성공 (데이터 있음)
return ApiResponse.ok(data);

// 성공 (데이터 없음)
return ApiResponse.ok();

// 실패
return ApiResponse.fail(errorCode.getCode(), message);
```

### 응답 JSON 구조

```json
// 성공
{
  "success": true,
  "message": "success",
  "data": { }
}

// 실패
{
  "success": false,
  "message": "프로젝트를 찾을 수 없습니다.",
  "errorCode": "P001"
}
```

---

## 예외 처리

### BusinessException 사용 패턴

```java
// Service에서 예외 던지기
throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);

// 메시지 커스텀
throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "해당 프로젝트가 존재하지 않습니다.");
```

### ErrorCode 추가 패턴

```java
// ErrorCode.java enum에 추가
// 형식: 도메인명_설명(HttpStatus, "코드", "메시지")
PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "프로젝트를 찾을 수 없습니다."),

// 코드 prefix 규칙
// C: Common / U: User / P: Project
// B: BusinessProcess / A: Analysis / F: File / AG: Agent
```

---

## 인증 / 보안

### JWT 흐름

```
로그인 → Access Token(1h) + Refresh Token(7d) 발급
       → TB_USERS에 Refresh Token + 만료일시 저장
API 요청 → Authorization: Bearer {AccessToken}
         → JwtFilter → SecurityContext 저장
Access Token 만료 → Refresh Token으로 재발급 요청
                  → 로그인 시 Refresh Token도 재발급
```

### JwtProvider 사용 패턴

```java
// 토큰 생성
String accessToken  = jwtProvider.generateAccessToken(userId, role);
String refreshToken = jwtProvider.generateRefreshToken(userId);

// 토큰 검증
boolean isValid = jwtProvider.validateToken(token);

// 토큰에서 정보 추출
String userId = jwtProvider.getUserId(token);
String role   = jwtProvider.getRole(token);
Date   expDt  = jwtProvider.getExpiration(token);
```

### PUBLIC_URLS (인증 없이 접근 가능)

```java
// SecurityConfig.java에서 관리
private static final String[] PUBLIC_URLS = {
    "/api/auth/**",      // 로그인, 회원가입, 토큰 재발급
    "/actuator/health",
    "/actuator/info",
};
// 새 공개 URL 추가 시 여기에만 추가
```

### 현재 로그인 유저 ID 가져오기

```java
// Controller / Service에서
String userId = (String) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();
```

### 비밀번호 암호화

```java
// BCryptPasswordEncoder Bean 사용
@RequiredArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;

    // 암호화
    String encoded = passwordEncoder.encode(rawPassword);

    // 검증
    boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
}
```

---

## 채번 (ID 생성)

### 채번 규칙

```
{PREFIX}-{yyyyMMdd}-{SEQ(3자리)}
예) PRJ-20260615-001
```

### PREFIX 규칙

| 테이블              | PREFIX |
| ------------------- | ------ |
| TB_PROJECT          | PRJ    |
| TB_BUSINESS_PROCESS | BUSIN  |
| TB_INPUT_DATA       | INP    |
| TB_ANALYSIS         | ANL    |
| TB_RESULT_TEXT      | TXT    |
| TB_RESULT_DIAGRAM   | DGR    |
| TB_PROCESS_DEFINE   | DEFIN  |

### 사용 패턴

```java
@RequiredArgsConstructor
public class ProjectService {
    private final IdGenerator idGenerator;

    public void createProject() {
        String projectNo = idGenerator.generateId("PRJ");
        // → PRJ-20260615-001
    }
}
```

---

## MyBatis 규칙

### Mapper 파일 위치

```
src/main/resources/mapper/{도메인명}/{도메인명}Mapper.xml
예) mapper/project/ProjectMapper.xml
    mapper/analysis/AnalysisMapper.xml
```

### Mapper 인터페이스 위치

```
com.{프로젝트명}.{도메인명}.repository.{도메인명}Mapper.java
```

### 핵심 규칙

```
- FK 없음 → 논리적 참조만 (조인 시 명시적 SQL 작성)
- PK = 비즈니스 채번 (fn_generate_key 사용)
- 컬럼명 snake_case → Java 필드명 camelCase 자동 변환
  (map-underscore-to-camel-case: true)
- 소프트 딜리트: USE_YN = 'N' (DELETE 쿼리 사용 금지)
```

### Mapper XML 기본 패턴

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.{프로젝트명}.{도메인명}.repository.{도메인명}Mapper">

    <select id="findById" parameterType="String" resultType="{도메인명}Response">
        SELECT *
        FROM TB_{테이블명}
        WHERE {PK} = #{id}
          AND USE_YN = 'Y'
    </select>

    <insert id="insert" parameterType="{도메인명}">
        INSERT INTO TB_{테이블명} (
            {PK}, ...컬럼들...,
            REG_ID, REG_DT
        ) VALUES (
            #{id}, ...값들...,
            #{regId}, NOW()
        )
    </insert>

    <update id="update" parameterType="{도메인명}">
        UPDATE TB_{테이블명}
        SET ...컬럼들...,
            MOD_ID = #{modId},
            MOD_DT = NOW()
        WHERE {PK} = #{id}
    </update>

    <!-- 삭제는 USE_YN = 'N' 처리 (물리 삭제 금지) -->
    <update id="delete" parameterType="String">
        UPDATE TB_{테이블명}
        SET USE_YN = 'N',
            MOD_ID = #{modId},
            MOD_DT = NOW()
        WHERE {PK} = #{id}
    </update>

</mapper>
```

---

## WebClient (Python FastAPI 호출)

### 사용 패턴

```java
@RequiredArgsConstructor
public class AgentCallService {

    @Qualifier("agentWebClient")
    private final WebClient agentWebClient;

    // 동기 호출
    public AgentResponse callAgent(AgentRequest request) {
        return agentWebClient.post()
                .uri("/v1/analyze")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AgentResponse.class)
                .block();
    }

    // 에러 처리 포함
    public AgentResponse callAgentWithFallback(AgentRequest request) {
        return agentWebClient.post()
                .uri("/v1/analyze")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is5xxServerError(),
                        res -> Mono.error(
                            new BusinessException(ErrorCode.AGENT_CALL_FAIL)))
                .bodyToMono(AgentResponse.class)
                .block();
    }
}
```

---

## 새 도메인 추가 시 체크리스트

```
새 도메인 추가 순서 (예: Project)

1. DTO 생성
   └── dto/request/ProjectCreateRequest.java
   └── dto/response/ProjectResponse.java

2. Entity 생성
   └── entity/Project.java

3. Mapper 인터페이스 생성
   └── repository/ProjectMapper.java

4. Mapper XML 생성
   └── resources/mapper/project/ProjectMapper.xml

5. Service 생성
   └── service/ProjectService.java

6. Controller 생성
   └── controller/ProjectController.java

7. ErrorCode 추가
   └── common/exception/ErrorCode.java에 도메인 에러 코드 추가
```

---

## 환경변수 규칙

### 절대 커밋 금지

```
application-prod.yml  → 운영 DB 정보
.env                  → 환경변수
*-secret.yml          → 시크릿 설정
```

### 환경변수 목록

| 변수명               | 설명                      |
| -------------------- | ------------------------- |
| DB_URL               | 운영 DB URL               |
| DB_USERNAME          | 운영 DB 유저명            |
| DB_PASSWORD          | 운영 DB 비밀번호          |
| JWT_SECRET           | JWT 시크릿 키 (32자 이상) |
| CORS_ALLOWED_ORIGINS | 허용 Origin (콤마 구분)   |
| AGENT_BASE_URL       | Python FastAPI URL        |

---

## 커밋 메시지 규칙

```
feat:     새 기능 추가
fix:      버그 수정
chore:    설정, 의존성, 빌드 관련
refactor: 코드 리팩토링 (기능 변경 없음)
docs:     문서 수정
test:     테스트 코드
style:    코드 포맷팅
```

## Git 규칙

- **절대 git commit / git push 하지 않는다**
- **절대 git add 하지 않는다**
- 코드 작성 완료 후 커밋 메시지만 제안한다
- 모든 Git 작업은 개발자가 직접 수행한다
