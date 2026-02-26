package com.crafthub.user_service.dto;

import com.crafthub.user_service.entity.enums.Role; // Не забудь імпорт!
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String phoneNumber; // 🆕 Нове поле
    private Role role;          // 🆕 Нове поле (BUYER, MILITARY_UNIT, SELLER)
}