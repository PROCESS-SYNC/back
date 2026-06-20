package com.springboot.framework.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)  // null 필드 JSON 미포함
public class ApiResponse<T> {

  private final boolean success;
  private final String  message;
  private final T       data;
  private final String  errorCode;

  // 성공 (데이터 있음)
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, "success", data, null);
  }

  // 성공 (데이터 없음)
  public static <T> ApiResponse<T> ok() {
    return new ApiResponse<>(true, "success", null, null);
  }

  // 실패
  public static <T> ApiResponse<T> fail(String errorCode, String message) {
    return new ApiResponse<>(false, message, null, errorCode);
  }

  private ApiResponse(boolean success, String message, T data, String errorCode) {
    this.success    = success;
    this.message    = message;
    this.data       = data;
    this.errorCode  = errorCode;
  }
  
}
