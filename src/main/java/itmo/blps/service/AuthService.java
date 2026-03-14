package itmo.blps.service;

import itmo.blps.dto.LoginRequest;
import itmo.blps.dto.LoginResponse;
import itmo.blps.dto.RegisterRequest;
import itmo.blps.dto.UserResponse;
import itmo.blps.entity.User;
import itmo.blps.entity.UserRole;
import itmo.blps.exception.BadRequestException;
import itmo.blps.repository.UserRepository;
import itmo.blps.security.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.getRole() == UserRole.ADMIN) {
            throw new BadRequestException("Admin cannot be registered via this endpoint");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user = userRepository.save(user);
        return UserResponse.from(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
        }
        String token = jwtUtils.generateToken(user);
        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtUtils.getExpirationMs() / 1000);
        response.setUser(UserResponse.from(user));
        return response;
    }

    public UserResponse me(User user) {
        return UserResponse.from(user);
    }
}
