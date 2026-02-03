package com.example.MedSafe.model.dto;

import com.example.MedSafe.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class AuthResponse {
    private Integer userId;
    private String userName;
    private String jwt;
    private Set<Role> roles;
}
