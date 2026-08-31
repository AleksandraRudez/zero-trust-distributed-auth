package com.zerotrust.serviceb.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse {
    private String status;
    private String message;
    private Object data;
    private long timestamp;

    public static ApiResponse success(String message, Object data) {
        return ApiResponse.builder()
                .status("SUCCESS")
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static ApiResponse error(String message) {
        return ApiResponse.builder()
                .status("ERROR")
                .message(message)
                .data(null)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}