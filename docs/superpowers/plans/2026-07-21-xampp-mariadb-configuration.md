# XAMPP MariaDB Configuration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the backend connect to the local XAMPP MariaDB `kgc_db` database without storing its password in the repository.

**Architecture:** Spring Boot resolves the datasource URL, username, and password from property placeholders. The URL and username retain XAMPP-local defaults, and the password defaults to empty only when `KGC_DB_PASSWORD` is absent. Hibernate's `update` schema mode remains explicitly enabled.

**Tech Stack:** Spring Boot, Spring Data JPA/Hibernate, MySQL Connector/J, XAMPP MariaDB 10.4.

---

### Task 1: Correct and externalize datasource configuration

**Files:**
- Modify: `F:\KGC\src\main\resources\application.properties:2-29`

- [ ] **Step 1: Inspect the existing datasource and JPA lines**

Run:

```powershell
$line=0; Get-Content F:\KGC\src\main\resources\application.properties | ForEach-Object { $line++; "${line}: $_" }
```

Expected: the current password and DDL values are visibly concatenated to comment lines instead of parsed as properties.

- [ ] **Step 2: Replace the malformed datasource and JPA block**

Set the following properties as standalone lines:

```properties
spring.datasource.url=${KGC_DB_URL:jdbc:mysql://127.0.0.1:3306/kgc_db?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true}
spring.datasource.username=${KGC_DB_USERNAME:root}
spring.datasource.password=${KGC_DB_PASSWORD:}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

- [ ] **Step 3: Verify that the malformed secret-bearing comment is absent**

Run:

```powershell
rg -n "^spring\.datasource\.(url|username|password)=|^spring\.jpa\.hibernate\.ddl-auto=" F:\KGC\src\main\resources\application.properties
```

Expected: four datasource properties and one `spring.jpa.hibernate.ddl-auto=update` line, each beginning at the start of its own line.

### Task 2: Record the configuration decision

**Files:**
- Modify: `F:\KGC\KGC_PROJECT_BASELINE.md:Change Log`

- [ ] **Step 1: Add the XAMPP MariaDB configuration entry**

Append a dated change-log row stating that the MySQL datasource now targets local XAMPP MariaDB `kgc_db`, reads credentials from environment variables, and restores explicit Hibernate `ddl-auto=update` schema creation.

- [ ] **Step 2: Verify the change log contains the entry**

Run:

```powershell
Get-Content F:\KGC\KGC_PROJECT_BASELINE.md -Tail 12
```

Expected: a 2026-07-21 row describing the XAMPP MariaDB configuration change.

### Task 3: Build and perform user-local connection verification

**Files:**
- No source changes.

- [ ] **Step 1: Build the backend**

Run:

```powershell
& 'C:\Users\吴育锦\.m2\wrapper\dists\apache-maven-3.9.11\03d7e36a140982eea48e22c1dcac01d8862b2550b2939e09a0809bbc5182a5bc\bin\mvn.cmd' -DskipTests package
```

Expected: Maven exits with code 0 and produces `target\KGC-0.0.1-SNAPSHOT.jar`.

- [ ] **Step 2: Start XAMPP MySQL and set the password in the launch shell**

Run in the user's PowerShell session:

```powershell
$env:KGC_DB_PASSWORD = 'your-XAMPP-root-password'
cd F:\KGC
java -jar target\KGC-0.0.1-SNAPSHOT.jar
```

Expected: startup logs show the service on port 8081 with no datasource authentication error.

- [ ] **Step 3: Verify the generated MariaDB schema**

Run in the MariaDB client after startup:

```sql
USE kgc_db;
SHOW TABLES;
DESCRIBE users;
```

Expected: `users` appears in `SHOW TABLES`, and `DESCRIBE users` includes persisted user fields such as `id`, `username`, `email`, `phone`, `password`, `role`, and `created_at` as determined by the current entity mapping.
