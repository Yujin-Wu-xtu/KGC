package com.wyj.kgc.controller;

import com.wyj.kgc.dto.LoginRequest;
import com.wyj.kgc.dto.RegisterRequest;
import com.wyj.kgc.entity.User;
import com.wyj.kgc.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        try {
            User newUser = userService.registerUser(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            String token = userService.login(loginRequest.getIdentifier(), loginRequest.getPassword());
            User user = userService.getUserByIdentifier(loginRequest.getIdentifier());

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "username", user.getUsername(),
                    "role", user.getRole().name()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "请先登录。"));
        }
        User user = userService.getUserByUsername(authentication.getName());
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("username", user.getUsername());
        profile.put("role", user.getRole().name());
        profile.put("email", user.getEmail());
        if (user.getTeacher() != null) {
            profile.put("teacherUsername", user.getTeacher().getUsername());
        } else {
            profile.put("teacherUsername", null);
        }
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me/teacher")
    public ResponseEntity<?> bindTeacher(Authentication authentication, @RequestBody Map<String, String> request) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "请先登录。"));
            }
            String teacherUsername = request.getOrDefault("teacherUsername", "");
            User updated = userService.setTeacher(authentication.getName(),
                    teacherUsername.isBlank() ? null : teacherUsername.trim());
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("message", updated.getTeacher() != null ? "绑定成功" : "已解绑");
            resp.put("teacherUsername", updated.getTeacher() != null ? updated.getTeacher().getUsername() : null);
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
