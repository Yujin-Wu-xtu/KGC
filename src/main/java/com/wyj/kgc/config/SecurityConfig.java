package com.wyj.kgc.config;

import com.wyj.kgc.security.JwtAuthenticationFilter; // ✅ 1. 导入我们的过滤器
import com.wyj.kgc.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // ✅ 导入原生过滤器类
import org.springframework.web.cors.CorsConfiguration; // ✅ CORS 配置
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

/**
 * Spring Security 的核心配置 "规则手册"
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    // ✅ 2. 新增字段：用于注入我们的 JWT 过滤器
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // ✅ 更新构造函数，注入两个依赖
    @Autowired
    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 密码加密器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证提供者
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * 安全过滤链 (配置的核心)
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 关闭 CSRF (对于无状态 API 通常不需要)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. 开启 CORS (允许跨域请求)
                .cors(org.springframework.security.config.Customizer.withDefaults())

                // 3. 配置 URL 授权规则
                .authorizeHttpRequests(authz -> authz
                        // 放行首页和所有静态资源
                        .requestMatchers("/", "/index.html", "/login.html", "/*.html", "/*.mp4", "/*.jpeg", "/*.jpg", "/*.png").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/assets/**", "/效果图/**").permitAll()
                        // 放行 "/api/v1/auth/**" (注册/登录)
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // 放行所有文件接口 (包括上传和解析)
                        .requestMatchers("/api/v1/files/**").permitAll()
                        // 放行错误页面
                        .requestMatchers("/error").permitAll()
                        // 放行图谱数据接口
                        .requestMatchers("/api/v1/graph/**").permitAll()
                        // 允许所有 OPTIONS 请求 (CORS 预检)
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        // 任何其他请求（主要是 API）都需要认证
                        .anyRequest().authenticated())

                // 4. 配置 Session 为无状态 (STATELESS)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 5. 指定认证提供者
                .authenticationProvider(authenticationProvider())

                // 6. 添加 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * ✅ CORS 配置源 — 让 .cors(withDefaults()) 能正确处理跨域预检请求
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*")); // 开发阶段允许所有来源
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}