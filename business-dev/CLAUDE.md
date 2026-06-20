# ProcessSync Business-Dev - CLAUDE.md

## 프로젝트 개요

ProcessSync 백엔드 API 서버.
AI 에이전트가 고객사 자료를 분석해서 개발 프로세스를 자동 정의 + 시각화해주는 서비스.

---

## 기술 스택

- **Java**: 17
- **Spring Boot**: 3.5.15
- **Build**: Gradle Groovy
- **DB**: PostgreSQL (포트 45432)
- **ORM**: MyBatis
- **Security**: Spring Security + JWT (jjwt 0.12.6)
- **HTTP Client**: WebClient (WebFlux)
- **AI Agent**: Python FastAPI (포트 8000)

---

## 패키지 구조

```
src/main/java/com/processsync/
├── common/                          # 공통 (framework에서 이식)
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── JwtFilter.java
│   │   ├── WebMvcConfig.java
│   │   └── WebClientConfig.java
│   ├── exception/
│   │   ├── ErrorCode.java
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   ├── response/
│   │   └── ApiResponse.java
│   └── util/
│       ├── JwtProvider.java
│       ├── IdGenerator.java
│       └── IdGeneratorMapper.java
│
├── user/                            # 사용자 도메인
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   └── entity/
│
├── project/                         # 프로젝트 도메인
├── business/                        # 비즈니스 프로세스 도메인
├── input/                           # 입력자료 도메인
├── analysis/                        # 분석 도메인
├── result/                          # 결과 도메인
└── define/                          # 확정 프로세스 도메인
```

---

## DB 정보

### 접속 정보 (개발)

```
Host: localhost
Port: 45432
DB:   processsync
User: psync_user
PW:   psync_pass
```

### 테이블 목록

```
TB_ID_SEQ                  채번 관리
TB_USERS                   사용자
TB_PROJECT                 프로젝트
TB_BUSINESS_PROCESS        비즈니스 프로세스
TB_INPUT_DATA              입력자료 (슈퍼타입)
TB_INPUT_FILE              입력자료 파일 (서브타입)
TB_INPUT_TEXT              입력자료 텍스트 (서브타입)
TB_ANALYSIS                분석
TB_ANALYSIS_INPUT_MAPPING  분석 ↔ 입력자료 N:M
TB_RESULT_TEXT             텍스트 결과
TB_RESULT_DIAGRAM          다이어그램 결과
TB_PROCESS_DEFINE          확정 프로세스
```

### DB 설계 원칙

```
- FK 없음 → 논리적 참조만
- PK = 비즈니스 채번 (fn_generate_key 사용)
- 컬럼명 snake_case → Java 필드명 camelCase 자동 변환
- 소프트 딜리트: USE_YN = 'N' (DELETE 쿼리 사용 금지)
- 감사 컬럼: REG_ID / REG_DT / MOD_ID / MOD_DT 모든 테이블 포함
```

### 채번 PREFIX 규칙

| 테이블              | PREFIX | 예시               |
| ------------------- | ------ | ------------------ |
| TB_PROJECT          | PRJ    | PRJ-20260615-001   |
| TB_BUSINESS_PROCESS | BUSIN  | BUSIN-20260615-001 |
| TB_INPUT_DATA       | INP    | INP-20260615-001   |
| TB_ANALYSIS         | ANL    | ANL-20260615-001   |
| TB_RESULT_TEXT      | TXT    | TXT-20260615-001   |
| TB_RESULT_DIAGRAM   | DGR    | DGR-20260615-001   |
| TB_PROCESS_DEFINE   | DEFIN  | DEFIN-20260615-001 |

---

## 공통 응답 포맷

### 항상 ApiResponse<T>로 반환

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
throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "커스텀 메시지");
```

### ErrorCode prefix 규칙

```
C:  Common       C001, C002 ...
U:  User         U001, U002 ...
P:  Project      P001, P002 ...
B:  Business     B001, B002 ...
A:  Analysis     A001, A002 ...
F:  File         F001, F002 ...
AG: Agent        AG001, AG002 ...
```

---

## 인증 / 보안

### JWT 흐름

```
로그인
→ Access Token (1시간) + Refresh Token (7일) 발급
→ TB_USERS에 Refresh Token + 만료일시 저장
→ 로그인 시 Refresh Token 재발급

API 요청
→ Authorization: Bearer {AccessToken}
→ JwtFilter → SecurityContext 저장

Access Token 만료
→ Refresh Token으로 재발급 요청
```

### PUBLIC_URLS (인증 없이 접근 가능)

```java
// SecurityConfig.java
private static final String[] PUBLIC_URLS = {
    "/api/auth/**",
    "/actuator/health",
    "/actuator/info",
};
```

### 현재 로그인 유저 ID

```java
String userId = (String) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();
```

---

## MyBatis 규칙

### Mapper 파일 위치

```
src/main/resources/mapper/{도메인}/{도메인}Mapper.xml
예) mapper/project/ProjectMapper.xml
    mapper/analysis/AnalysisMapper.xml
```

### Mapper XML 기본 패턴

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.processsync.{도메인}.repository.{도메인}Mapper">

    <select id="findById" parameterType="String" resultType="{도메인}Response">
        SELECT *
        FROM TB_{테이블명}
        WHERE {PK} = #{id}
          AND USE_YN = 'Y'
    </select>

    <insert id="insert" parameterType="{도메인}">
        INSERT INTO TB_{테이블명} (
            {PK}, REG_ID, REG_DT
        ) VALUES (
            #{id}, #{regId}, NOW()
        )
    </insert>

    <update id="update" parameterType="{도메인}">
        UPDATE TB_{테이블명}
        SET MOD_ID = #{modId},
            MOD_DT = NOW()
        WHERE {PK} = #{id}
    </update>

    <!-- 물리 삭제 금지 → USE_YN = 'N' 처리 -->
    <update id="delete">
        UPDATE TB_{테이블명}
        SET USE_YN = 'N',
            MOD_ID = #{modId},
            MOD_DT = NOW()
        WHERE {PK} = #{id}
    </update>

</mapper>
```

---

## 새 도메인 추가 시 체크리스트

```
1. Entity 생성
   └── {도메인}/entity/{도메인}.java

2. DTO 생성
   └── {도메인}/dto/request/{도메인}CreateRequest.java
   └── {도메인}/dto/response/{도메인}Response.java

3. Mapper 인터페이스 생성
   └── {도메인}/repository/{도메인}Mapper.java

4. Mapper XML 생성
   └── resources/mapper/{도메인}/{도메인}Mapper.xml

5. Service 생성
   └── {도메인}/service/{도메인}Service.java

6. Controller 생성
   └── {도메인}/controller/{도메인}Controller.java

7. ErrorCode 추가
   └── common/exception/ErrorCode.java
```

---

## Python FastAPI 연동

### AgentCallService 패턴

```java
@RequiredArgsConstructor
public class AgentCallService {

    @Qualifier("agentWebClient")
    private final WebClient agentWebClient;

    public AgentResponse callAgent(AgentRequest request) {
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

## STATUS 값 규칙

### TB_ANALYSIS

```
PENDING     분석 대기
PROCESSING  분석 중
COMPLETED   완료
FAILED      실패
CANCELLED   취소
```

### TB_RESULT_TEXT / TB_RESULT_DIAGRAM

```
DRAFT       임시 (AI 도출 직후)
CONFIRMED   확정
REJECTED    반려
```

---

## 환경변수 규칙

### 절대 커밋 금지

```
application-prod.yml
.env
*-secret.yml
```

### 환경변수 목록

| 변수명               | 설명                      |
| -------------------- | ------------------------- |
| DB_URL               | 운영 DB URL               |
| DB_USERNAME          | 운영 DB 유저명            |
| DB_PASSWORD          | 운영 DB 비밀번호          |
| JWT_SECRET           | JWT 시크릿 키 (32자 이상) |
| CORS_ALLOWED_ORIGINS | 허용 Origin               |
| AGENT_BASE_URL       | Python FastAPI URL        |

---

## Git 규칙

- **절대 git commit / git push 하지 않는다**
- **절대 git add 하지 않는다**
- 코드 작성 완료 후 커밋 메시지만 제안한다
- 모든 Git 작업은 개발자가 직접 수행한다

## 커밋 메시지 규칙

```
feat:     새 기능 추가
fix:      버그 수정
chore:    설정, 의존성, 빌드 관련
refactor: 코드 리팩토링
docs:     문서 수정
test:     테스트 코드
style:    코드 포맷팅
```
