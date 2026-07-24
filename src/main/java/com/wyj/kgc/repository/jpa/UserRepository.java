package com.wyj.kgc.repository.jpa;

import com.wyj.kgc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * 用户 (User) 的数据访问接口 (DAO)
 *
 * JpaRepository 会自动为我们提供 CRUD (增删改查) 方法，例如:
 * - save(User user): 保存或更新一个用户
 * - findById(Long id): 按ID查找用户
 * - deleteById(Long id): 按ID删除用户
 * - findAll(): 查找所有用户
 * ... 以及更多！
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Spring Data JPA 的 "魔法" 之一：
     * 我们只需要按照 "findBy[字段名]" 的格式定义这个方法，
     * Spring Boot 就会自动为我们实现一个根据用户名查找用户的功能。
     *
     * @param username 要查找的用户名
     * @return 一个 Optional<User>，它可能包含用户，也可能为空 (如果没找到)
     */
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);
}
