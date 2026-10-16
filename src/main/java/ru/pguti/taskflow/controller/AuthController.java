package ru.pguti.taskflow.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.pguti.taskflow.dto.request.RegisterRequest;
import ru.pguti.taskflow.service.UserService;
import ru.pguti.taskflow.dto.request.LoginRequest;
import ru.pguti.taskflow.dto.response.LoginResponse;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public String register(@RequestBody @Valid RegisterRequest request) {
        userService.register(request);
        return "Пользователь зарегистрирован";
    }
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        String token = userService.login(request);

        return new LoginResponse(token);
    }
}