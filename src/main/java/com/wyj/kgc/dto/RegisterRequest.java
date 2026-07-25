package com.wyj.kgc.dto;

import com.wyj.kgc.entity.UserRole;

/**
 * "数据传输对象" (DTO)
 * 专门用于在 "Controller" 层接收前端发来的 "注册" 请求的 JSON 数据
 */
public class RegisterRequest {
    private String username;
    private String email;
    private String phone;
    private String emailCode;
    private String phoneCode;
    private String password;
    private UserRole role;

    // --- Getters and Setters (Lombok @Data 也可以) ---
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmailCode() {
        return emailCode;
    }

    public void setEmailCode(String emailCode) {
        this.emailCode = emailCode;
    }

    public String getPhoneCode() {
        return phoneCode;
    }

    public void setPhoneCode(String phoneCode) {
        this.phoneCode = phoneCode;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
