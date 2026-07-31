package com.wyj.kgc.service;

import com.wyj.kgc.dto.RegisterRequest;
import com.wyj.kgc.entity.User;
import com.wyj.kgc.entity.UserRole;
import com.wyj.kgc.repository.jpa.UserRepository;
import com.wyj.kgc.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{6,20}$");

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeService verificationCodeService;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            VerificationCodeService verificationCodeService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationCodeService = verificationCodeService;
    }

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationCodeService = null;
    }

    /**
     * Legacy registration entry point retained for existing callers.
     * New registrations must provide an email address or mobile number.
     */
    public User registerUser(String username, String plainPassword, UserRole role) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(plainPassword);
        request.setRole(role);
        return registerUser(request);
    }

    public User registerUser(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request must not be empty.");
        }

        String email = normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhone());
        if (email == null && phone == null) {
            throw new IllegalArgumentException("Please provide an email address or mobile number.");
        }

        String username = normalizeUsername(request.getUsername());
        if (username == null) {
            username = email != null ? email : phone;
        }

        String password = request.getPassword();
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must contain at least 6 characters.");
        }

        verifyRegistrationCodesIfConfigured(request, email, phone);

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("This username is already in use.");
        }
        if (email != null && userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("This email address is already registered.");
        }
        if (phone != null && userRepository.findByPhone(phone).isPresent()) {
            throw new IllegalArgumentException("This mobile number is already registered.");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(request.getRole() == null ? UserRole.ROLE_STUDENT : request.getRole());
        return userRepository.save(newUser);
    }

    private void verifyRegistrationCodesIfConfigured(RegisterRequest request, String email, String phone) {
        if (verificationCodeService == null) {
            return;
        }
        if (email != null) {
            String emailCode = normalizeIdentifier(request.getEmailCode());
            if (emailCode == null) {
                throw new IllegalArgumentException("Please provide the email verification code.");
            }
            verificationCodeService.verify("email", email, emailCode);
        }
        if (phone != null) {
            String phoneCode = normalizeIdentifier(request.getPhoneCode());
            if (phoneCode == null) {
                throw new IllegalArgumentException("Please provide the mobile verification code.");
            }
            verificationCodeService.verify("sms", phone, phoneCode);
        }
    }

    public String login(String identifier, String password) {
        User user = findUserByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Invalid account or password."));

        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid account or password.");
        }

        return jwtTokenProvider.createToken(user.getUsername());
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist."));
    }

    public User bindTeacher(String studentUsername, String teacherUsername) {
        User student = getUserByUsername(studentUsername);
        User teacher = getUserByUsername(teacherUsername);
        student.setTeacher(teacher);
        return userRepository.save(student);
    }

    public User getUserByIdentifier(String identifier) {
        return findUserByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist."));
    }

    private Optional<User> findUserByIdentifier(String identifier) {
        String normalizedIdentifier = normalizeIdentifier(identifier);
        if (normalizedIdentifier == null) {
            return Optional.empty();
        }

        Optional<User> byUsername = userRepository.findByUsername(normalizedIdentifier);
        if (byUsername.isPresent()) {
            return byUsername;
        }

        if (normalizedIdentifier.contains("@")) {
            String email = normalizeEmail(normalizedIdentifier);
            return userRepository.findByEmail(email);
        }

        String compactPhone = compactPhone(normalizedIdentifier);
        if (PHONE_PATTERN.matcher(compactPhone).matches()) {
            return userRepository.findByPhone(compactPhone);
        }

        return Optional.empty();
    }

    private String normalizeUsername(String value) {
        String normalized = normalizeIdentifier(value);
        if (normalized != null && normalized.length() > 254) {
            throw new IllegalArgumentException("Username is too long.");
        }
        return normalized;
    }

    private String normalizeEmail(String value) {
        String normalized = normalizeIdentifier(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid email address.");
        }
        return normalized;
    }

    private String normalizePhone(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = compactPhone(value);
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid mobile number.");
        }
        return normalized;
    }

    private String compactPhone(String value) {
        return value.trim().replaceAll("[\\s-]", "");
    }

    private String normalizeIdentifier(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
