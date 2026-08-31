package com.zerotrust.servicea.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Claims {
    private String userId;
    private String role;
    private String clientType;
    private long iat;  // Issued at
}