package com.wyj.kgc.entity;

// 导入 Spring Boot 3.x 使用的 jakarta persistence
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
// 导入我们前面添加的 Lombok 依赖
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet; // <-- 新增导入
import java.util.Set;     // <-- 新增导入

/**
 * 用户实体类
 * 这将自动在 MySQL 中映射为一张名为 'users' 的表
 */
@Data // Lombok: 自动生成 getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: 自动生成一个无参数的构造函数
@AllArgsConstructor // Lombok: 自动生成一个包含所有字段的构造函数
@Entity // JPA: 声明这是一个实体类，它会和数据库表做映射
@Table(name = "users") // JPA: 指定表名为 "users" (比 "user" 更安全，"user"是SQL保留字)
public class User {

    @Id // JPA: 标记这是主键
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB: 允许数据库自动生成主键 (自增)
    private Long id;

    @Column(nullable = false, unique = true, length = 254) // DB: 不允许为空, 必须唯一, 最大长度254
    private String username;

    @Column(unique = true, length = 254)
    private String email;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(nullable = false) // DB: 不允许为空
    @JsonIgnore
    private String password; // 注意: 在实际项目中，这里存储的应该是被加密后的哈希值

    @Enumerated(EnumType.STRING) // JPA: 告诉JPA在数据库中把枚举存为字符串
    @Column(nullable = false, length = 20) // DB: 不允许为空
    private UserRole role; // 关联我们刚刚定义的 UserRole 枚举

    @Column(updatable = false) // DB: 创建后不允许更新
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // --- ↓↓↓ 关键修改在这里 ↓↓↓ ---
    /**
     * 声明一个 "一对多" 的关系：
     * 一个 User 实体可以对应多个 ResourceFile 实体。
     * "mappedBy = "user"" 告诉JPA：
     * "这个关系的配置(外键)在 ResourceFile 类的 'user' 字段上。"
     * "我 (User表) 只是被动的一方，不要在我这张表里创建外键。"
     *
     * fetch = FetchType.LAZY 是性能优化，默认不加载这个文件列表。
     * cascade = CascadeType.ALL 表示级联操作（如删除用户时，也删除他的所有文件）。
     */
    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private Set<ResourceFile> files = new HashSet<>();
    // --- ↑↑↑ 关键修改完毕 ↑↑↑ ---


    // --- JPA 生命周期回调 ---

    /**
     * 在实体被持久化 (保存) 之前自动调用
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * 在实体被更新之前自动调用
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
