package com.crafthub.user_service.dto;

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
    // Ми не дозволяємо юзеру вказувати роль при реєстрації,
    // вона буде 'USER' за замовчуванням
}