package cn.aioi.problem.service;

import cn.aioi.problem.api.dto.AuthDtos;
import cn.aioi.problem.domain.User;
import cn.aioi.problem.repository.UserRepository;
import cn.aioi.problem.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String username = request.username().trim();
        if (users.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = users.save(new User(username, passwordEncoder.encode(request.password())));
        return authResponse(user);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        if (!authentication.isAuthenticated()) {
            throw new BadCredentialsException("Bad credentials");
        }
        User user = users.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Bad credentials"));
        return authResponse(user);
    }

    public AuthDtos.UserResponse me(User user) {
        return new AuthDtos.UserResponse(user.getId(), user.getUsername());
    }

    private AuthDtos.AuthResponse authResponse(User user) {
        return new AuthDtos.AuthResponse(jwtService.issue(user), new AuthDtos.UserResponse(user.getId(), user.getUsername()));
    }
}

