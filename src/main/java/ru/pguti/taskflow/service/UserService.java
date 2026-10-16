package ru.pguti.taskflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.pguti.taskflow.dto.request.RegisterRequest;
import ru.pguti.taskflow.entity.User;
import ru.pguti.taskflow.dto.request.LoginRequest;
import ru.pguti.taskflow.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import ru.pguti.taskflow.security.JwtService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Неверный email или пароль")
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new RuntimeException("Неверный email или пароль");
        }

        return jwtService.generateToken(user.getEmail());
    }
    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Пользователь уже существует");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
    }
}