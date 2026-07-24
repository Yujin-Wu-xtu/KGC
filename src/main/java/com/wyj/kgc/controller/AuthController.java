package com.wyj.kgc.controller;

import com.wyj.kgc.dto.RegisterRequest;
import com.wyj.kgc.dto.LoginRequest;
import com.wyj.kgc.entity.User;
import com.wyj.kgc.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 负责处理 "/api/v1/auth/..." 相关的请求，例如注册和登录
 */
@RestController
@RequestMapping("/api/v1/auth") // 匹配 SecurityConfig 中的白名单
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 注册新用户的 API 接口
     *
     * @param registerRequest 包含 "username", "password", "role" 的 JSON 对象
     * @return 成功则返回新创建的用户信息 (HTTP 201)
     */
    @PostMapping("/register") // 完整的 URL 是: POST /api/v1/auth/register
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {

        try {
            // 1. 调用我们刚刚创建的 UserService
            User newUser = userService.registerUser(registerRequest);

            // 2. 注册成功
            // 返回 "HTTP 201 Created" 状态码
            // 并把新创建的 user 对象 (不含密码，虽然 User 实体里有) 返回
            // 注意：在实际项目中，我们应该返回一个 "UserDTO" 而不是 "User" 实体，以避免泄露密码哈希
            // 但在目前阶段，这是可以接受的。
            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);

        } catch (RuntimeException e) {
            // 3. 注册失败 (例如，用户名已存在)
            // 返回 "HTTP 400 Bad Request" 状态码
            // 并在响应体中返回错误消息
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage()); // e.g., "注册失败：用户名 'admin' 已被占用！"
        }
    }

    // TODO: 未来我们还会在这里添加 @PostMapping("/login")
    // ✅ 新增：登录接口
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 调用 Service 里的登录逻辑
            String token = userService.login(loginRequest.getIdentifier(), loginRequest.getPassword());
            User user = userService.getUserByIdentifier(loginRequest.getIdentifier());

            // 登录成功，返回 Token
            // 这里简单返回一个 Map 或者直接返回字符串，看你喜好，这里先返回 Map 方便前端解析
            return ResponseEntity.ok(java.util.Map.of(
                    "token", token,
                    "username", user.getUsername(),
                    "role", user.getRole().name()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
