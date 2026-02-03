package com.example.MedSafe.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDto {
    private String username;    // Имя пользователя
    private String email;       // Электронная почта
    private String password;    // Пароль пользователя (в незашифрованном виде)
}

