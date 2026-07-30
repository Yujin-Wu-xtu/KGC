# Verification Code Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add real email and SMS verification-code flows to registration, using low-cost SMTP email and Tencent Cloud SMS, with code validation before account creation.

**Architecture:** Keep registration/login behavior intact, but insert a small verification-code subsystem in front of registration. Store code records in MySQL so retries, expiry, and audit are visible after restart. Route email through Spring `JavaMailSender` and route SMS through a small Tencent Cloud adapter that wraps the official SMS SDK.

**Tech Stack:** Spring Boot 3.2.3, Java 17, Spring Mail, Spring Data JPA, Tencent Cloud SMS SDK 3.x, MySQL, Spring MVC, Spring Security, Mockito, MockMvc.

---

### Task 1: Add mail and SMS dependencies plus externalized config

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.properties`
- Add: `.env.example`
- Add: `src/main/java/com/wyj/kgc/config/VerificationCodeProperties.java`

- [ ] **Step 1: Write the failing configuration test**

```java
@SpringBootTest
class VerificationCodePropertiesTest {
    @Autowired
    private VerificationCodeProperties properties;

    @Test
    void bindsVerificationCodeSettings() {
        assertEquals(300, properties.getTtlSeconds());
        assertEquals(60, properties.getCooldownSeconds());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -Dtest=VerificationCodePropertiesTest test`
Expected: fail because the properties bean and mail/SMS dependencies do not exist yet.

- [ ] **Step 3: Add the minimal implementation**

Add `spring-boot-starter-mail` and Tencent Cloud SMS SDK to `pom.xml`. Add `@ConfigurationProperties(prefix = "verification-code")` with fields for TTL, cooldown, daily limit, sender address, SMS app id, sign, and template id. Bind secrets from environment variables only.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -Dtest=VerificationCodePropertiesTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/resources/application.properties .env.example src/main/java/com/wyj/kgc/config/VerificationCodeProperties.java src/test/java/com/wyj/kgc/VerificationCodePropertiesTest.java
git commit -m "feat: add verification code configuration"
```

### Task 2: Persist verification-code records in MySQL

**Files:**
- Add: `src/main/java/com/wyj/kgc/entity/VerificationCode.java`
- Add: `src/main/java/com/wyj/kgc/repository/jpa/VerificationCodeRepository.java`
- Add: `src/main/java/com/wyj/kgc/service/VerificationCodeService.java`
- Add: `src/test/java/com/wyj/kgc/VerificationCodeServiceTest.java`

- [ ] **Step 1: Write the failing service test**

```java
@Test
void createsAndValidatesActiveEmailCode() {
    VerificationCode code = verificationCodeService.issue("email", "student@example.com");
    assertTrue(verificationCodeService.verify("email", "student@example.com", code.getCode()));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -Dtest=VerificationCodeServiceTest test`
Expected: fail because the entity, repository, and service are missing.

- [ ] **Step 3: Implement the persistence model**

Create a `VerificationCode` entity with `id`, `channel`, `target`, `codeHash`, `expiresAt`, `usedAt`, `sendCountToday`, `lastSentAt`, `createdAt`, and `updatedAt`. Add repository queries for active codes by channel and target. Implement `issue(...)`, `verify(...)`, `markUsed(...)`, and cooldown/daily-limit checks.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -Dtest=VerificationCodeServiceTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wyj/kgc/entity/VerificationCode.java src/main/java/com/wyj/kgc/repository/jpa/VerificationCodeRepository.java src/main/java/com/wyj/kgc/service/VerificationCodeService.java src/test/java/com/wyj/kgc/VerificationCodeServiceTest.java
git commit -m "feat: persist verification codes"
```

### Task 3: Add email delivery through Spring Mail

**Files:**
- Add: `src/main/java/com/wyj/kgc/service/VerificationCodeSender.java`
- Add: `src/main/java/com/wyj/kgc/service/EmailVerificationCodeSender.java`
- Add: `src/test/java/com/wyj/kgc/EmailVerificationCodeSenderTest.java`

- [ ] **Step 1: Write the failing sender test**

```java
@Test
void sendsSixDigitCodeByEmail() {
    EmailVerificationCodeSender sender = new EmailVerificationCodeSender(mailSender, properties);
    sender.send("student@example.com", "123456");
    verify(mailSender).send(any(SimpleMailMessage.class));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -Dtest=EmailVerificationCodeSenderTest test`
Expected: fail because the sender class does not exist yet.

- [ ] **Step 3: Implement the sender**

Use `JavaMailSender` and `SimpleMailMessage` to send a short text email with the code and expiry window. Keep the subject simple and avoid HTML complexity.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -Dtest=EmailVerificationCodeSenderTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wyj/kgc/service/VerificationCodeSender.java src/main/java/com/wyj/kgc/service/EmailVerificationCodeSender.java src/test/java/com/wyj/kgc/EmailVerificationCodeSenderTest.java
git commit -m "feat: send verification codes by email"
```

### Task 4: Add Tencent Cloud SMS delivery through a thin adapter

**Files:**
- Add: `src/main/java/com/wyj/kgc/service/TencentSmsVerificationCodeSender.java`
- Add: `src/main/java/com/wyj/kgc/config/TencentSmsConfig.java`
- Add: `src/test/java/com/wyj/kgc/TencentSmsVerificationCodeSenderTest.java`

- [ ] **Step 1: Write the failing SMS sender test**

```java
@Test
void sendsSixDigitCodeBySms() {
    TencentSmsVerificationCodeSender sender = new TencentSmsVerificationCodeSender(smsClient, properties);
    sender.send("13800138000", "123456");
    verify(smsClient).SendSms(any(SendSmsRequest.class));
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -Dtest=TencentSmsVerificationCodeSenderTest test`
Expected: fail because the adapter class does not exist yet.

- [ ] **Step 3: Implement the adapter**

Wrap Tencent Cloud `SmsClient` from `com.tencentcloudapi.sms.v20210111`. Configure it from environment variables, and map the verification template parameters so the API only sends a code plus expiry text.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -Dtest=TencentSmsVerificationCodeSenderTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wyj/kgc/service/TencentSmsVerificationCodeSender.java src/main/java/com/wyj/kgc/config/TencentSmsConfig.java src/test/java/com/wyj/kgc/TencentSmsVerificationCodeSenderTest.java
git commit -m "feat: send verification codes by sms"
```

### Task 5: Expose send and verify APIs and wire them into registration

**Files:**
- Modify: `src/main/java/com/wyj/kgc/controller/AuthController.java`
- Modify: `src/main/java/com/wyj/kgc/dto/RegisterRequest.java`
- Add: `src/main/java/com/wyj/kgc/dto/VerificationCodeSendRequest.java`
- Add: `src/main/java/com/wyj/kgc/dto/VerificationCodeVerifyRequest.java`
- Add: `src/main/java/com/wyj/kgc/controller/VerificationCodeController.java`
- Modify: `src/main/java/com/wyj/kgc/service/UserService.java`
- Add: `src/test/java/com/wyj/kgc/VerificationCodeControllerTest.java`
- Add: `src/test/java/com/wyj/kgc/UserServiceVerificationCodeRegistrationTest.java`

- [ ] **Step 1: Write the failing controller and service tests**

```java
@Test
void sendEmailCodeReturnsOk() throws Exception {
    mockMvc.perform(post("/api/v1/auth/verification-code/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"channel":"email","target":"student@example.com"}
            """))
        .andExpect(status().isOk());
}
```

```java
@Test
void registerRequiresMatchingCode() {
    RegisterRequest request = new RegisterRequest();
    request.setEmail("student@example.com");
    request.setPassword("password123");
    request.setEmailCode("123456");
    assertDoesNotThrow(() -> userService.registerUser(request));
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -Dtest=VerificationCodeControllerTest,UserServiceVerificationCodeRegistrationTest test`
Expected: fail because the controller DTO fields and verification flow do not exist yet.

- [ ] **Step 3: Implement the API flow**

Add `/api/v1/auth/verification-code/send` and `/api/v1/auth/verification-code/verify`. Extend registration so an email registration requires `emailCode`, and a phone registration requires `phoneCode`. Keep username/password login unchanged.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `mvn -Dtest=VerificationCodeControllerTest,UserServiceVerificationCodeRegistrationTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wyj/kgc/controller/AuthController.java src/main/java/com/wyj/kgc/dto/RegisterRequest.java src/main/java/com/wyj/kgc/dto/VerificationCodeSendRequest.java src/main/java/com/wyj/kgc/dto/VerificationCodeVerifyRequest.java src/main/java/com/wyj/kgc/controller/VerificationCodeController.java src/main/java/com/wyj/kgc/service/UserService.java src/test/java/com/wyj/kgc/VerificationCodeControllerTest.java src/test/java/com/wyj/kgc/UserServiceVerificationCodeRegistrationTest.java
git commit -m "feat: require verification codes for registration"
```

### Task 6: Update the registration page to request and submit codes

**Files:**
- Modify: `src/main/resources/static/register.html`

- [ ] **Step 1: Write the UI behavior test by manual inspection**

The page must show:
- one button for sending email code
- one button for sending SMS code
- one input for the received code
- one submit flow that includes the code fields in the registration payload

- [ ] **Step 2: Implement the page update**

Keep the existing register form, add send-code buttons next to email and phone inputs, disable them during cooldown, and call the new `/api/v1/auth/verification-code/send` endpoint before submission.

- [ ] **Step 3: Verify the page behavior**

Run the app and check that the browser can:
- request a code
- see cooldown feedback
- submit registration with the code field present

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/register.html
git commit -m "feat: add verification code fields to registration page"
```

### Task 7: Update baseline and deployment notes

**Files:**
- Modify: `KGC_PROJECT_BASELINE.md`
- Add or modify: `docs/superpowers/specs/2026-07-24-verification-code-integration-design.md` if the implementation diverges from this plan

- [ ] **Step 1: Record the verification-code capability**

Capture the chosen providers, environment variables, TTL, cooldown, and the fact that registration now requires a verified email or phone number.

- [ ] **Step 2: Check for stale documentation**

Search for any text that still says the registration page does not send codes, and update it.

- [ ] **Step 3: Commit**

```bash
git add KGC_PROJECT_BASELINE.md
git commit -m "docs: record verification code registration flow"
```

### Self-Review

- Coverage check: email sending, SMS sending, persistence, API surface, registration integration, and frontend updates are all covered.
- Placeholder check: no TBD/TODO placeholders remain in the plan.
- Consistency check: the same `channel`, `target`, `code`, `emailCode`, and `phoneCode` names are used throughout.
- Risk check: external provider credentials stay in environment variables, not in browser code or committed config.

