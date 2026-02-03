package com.example.MedSafe.service;

import com.example.MedSafe.model.User;
import com.example.MedSafe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public String deleteById(Integer userId) {
        userRepository.deleteById(userId);
        return "Успешное удаление";
    }
}
