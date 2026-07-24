package com.wyj.kgc.service;

import com.wyj.kgc.entity.User;
import com.wyj.kgc.repository.jpa.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 【关键桥梁】
 * 这个 Service 专门用于 "连接" 你的 User 实体和 Spring Security 的 UserDetails
 * 它实现了 UserDetailsService 接口，重写了 loadUserByUsername 方法。
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 这是 Spring Security 在进行"认证"(Authentication)时调用的【唯一】方法
     *
     * @param username 前端登录时传来的用户名 (e.g., "teacher_wu")
     * @return 一个 Spring Security 能理解的 UserDetails 对象
     * @throws UsernameNotFoundException 如果在数据库中找不到该用户
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 使用我们自己的 UserRepository 去数据库中查找 "User" 实体
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("用户未找到，用户名: " + username)
                );

        // 2. 将我们自定义的 "UserRole" (e.g., ROLE_TEACHER) 转换为
        //    Spring Security 能理解的 "GrantedAuthority"
        Set<GrantedAuthority> authorities = Set.of(
                new SimpleGrantedAuthority(user.getRole().name())
        );

        // 3. 【关键】返回 Spring Security 内置的 "User" 对象
        // 它需要：用户名、哈希密码、权限列表
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(), // 数据库中存储的【哈希密码】
                authorities
        );
    }
}