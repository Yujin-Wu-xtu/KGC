package com.wyj.kgc;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// 导入这两个新注解
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;



// 告诉 Spring Boot: "所有 JPA/MySQL 的 Repository, 请只去 '...repository.jpa' 包里找！"
@EnableJpaRepositories(basePackages = "com.wyj.kgc.repository.jpa")

// 告诉 Spring Boot: "所有 Neo4j 的 Repository, 请只去 '...repository.neo4j' 包里找！"
@EnableNeo4jRepositories(basePackages = "com.wyj.kgc.repository.neo4j")
@SpringBootApplication
@EntityScan(basePackages = "com.wyj.kgc.entity")
public class KgcApplication {

    public static void main(String[] args) {
        SpringApplication.run(KgcApplication.class, args);
    }

}
