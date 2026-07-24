# XAMPP MariaDB Connection Configuration Design

## Scope

Connect the Spring Boot prototype to the user's local XAMPP MariaDB instance at `127.0.0.1:3306/kgc_db`. The database already exists and is intentionally empty.

## Chosen Design

The datasource URL and username will have safe local defaults, while the password will be supplied by the `KGC_DB_PASSWORD` environment variable at process startup. The configuration will retain `spring.jpa.hibernate.ddl-auto=update` as an actual property, so Hibernate creates and evolves entity tables such as `users` on the first successful connection.

This replaces the current malformed commented lines, where the password and DDL setting are not parsed as properties. It does not change the database contents, entity mapping, API behavior, Neo4j configuration, or model-provider configuration.

## Runtime Flow

1. The user starts MySQL from XAMPP.
2. The user sets `KGC_DB_PASSWORD` in the same PowerShell session, without placing the password in source control.
3. Spring Boot resolves the datasource placeholders and connects to `kgc_db`.
4. Hibernate applies the current entity schema using `update`.
5. The user verifies the generated tables using MariaDB commands.

## Errors and Verification

An incorrect password or stopped XAMPP service causes a datasource-connection error in the application log; the code will not silently fall back to another database. Verification consists of inspecting the resolved properties without secrets, building the application, starting it locally with the user's own password, and confirming `SHOW TABLES` / `DESCRIBE users` in MariaDB.
