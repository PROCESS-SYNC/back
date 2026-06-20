package com.springboot.framework.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
  
  // --- Common
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST,             "C001", "입력값이 올바르지 않습니다."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED,                   "C003", "인증이 필요합니다."),
  FORBIDDEN(HttpStatus.FORBIDDEN,                         "C004", "접근 권한이 없습니다."),

  // ── User
  USER_NOT_FOUND(HttpStatus.NOT_FOUND,                    "U001", "사용자를 찾을 수 없습니다."),
  DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT,                 "U002", "이미 사용 중인 아이디입니다."),
  DUPLICATE_EMAIL(HttpStatus.CONFLICT,                    "U003", "이미 사용 중인 이메일입니다."),
  INVALID_PASSWORD(HttpStatus.UNAUTHORIZED,               "U004", "비밀번호가 올바르지 않습니다."),

  // ── Project
  PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND,                 "P001", "프로젝트를 찾을 수 없습니다."),

  // ── Business Process
  BUSINESS_PROCESS_NOT_FOUND(HttpStatus.NOT_FOUND,        "B001", "비즈니스 프로세스를 찾을 수 없습니다."),

  // ── Analysis
  ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND,                "A001", "분석을 찾을 수 없습니다."),
  ANALYSIS_IN_PROGRESS(HttpStatus.CONFLICT,               "A002", "분석이 진행 중입니다."),

  // ── File
  FILE_UPLOAD_FAIL(HttpStatus.INTERNAL_SERVER_ERROR,      "F001", "파일 업로드에 실패했습니다."),
  FILE_NOT_FOUND(HttpStatus.NOT_FOUND,                    "F002", "파일을 찾을 수 없습니다."),
  INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST,               "F003", "지원하지 않는 파일 형식입니다."),
  FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST,              "F004", "파일 크기가 초과되었습니다."),

  // ── Agent (Python FastAPI)
  AGENT_CALL_FAIL(HttpStatus.INTERNAL_SERVER_ERROR,       "AG001", "AI 에이전트 호출에 실패했습니다."),
  AGENT_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT,               "AG002", "AI 에이전트 응답 시간이 초과되었습니다.");

  private final HttpStatus  httpStatus;
  private final String      code;
  private final String      message;

  ErrorCode(HttpStatus httpStatus, String code, String message) {
    this.httpStatus = httpStatus;
    this.code       = code;
    this.message    = message;
  }
}
