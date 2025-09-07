package com.example.shopBackend.service;

import com.example.shopBackend.dto.LoginRequest;
import com.example.shopBackend.dto.Response;
import com.example.shopBackend.dto.UserDto;
import com.example.shopBackend.entity.User;
import com.example.shopBackend.enums.UserRole;
import com.example.shopBackend.exceptions.InvalidCredentialsException;
import com.example.shopBackend.exceptions.NotFoundException;
import com.example.shopBackend.security.XssSanitizer;
import jakarta.validation.ValidationException;

import com.example.shopBackend.mapper.EntityDtoMapper;
import com.example.shopBackend.repository.UserRepo;
import com.example.shopBackend.security.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private EntityDtoMapper entityDtoMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private XssSanitizer xssSanitizer;

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final int MAX_PHONE_LENGTH = 20;
    private static final int MIN_PASSWORD_LENGTH = 6;

    @Override
    public Response registerUser(UserDto registrationRequest) {
        if (registrationRequest == null) {
            throw new ValidationException("Registration data required");
        }

        // basic validation
        if (!StringUtils.hasText(registrationRequest.getName()) || registrationRequest.getName().length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Invalid name");
        }
        if (!StringUtils.hasText(registrationRequest.getEmail()) || registrationRequest.getEmail().length() > MAX_EMAIL_LENGTH) {
            throw new ValidationException("Invalid email");
        }
        if (!StringUtils.hasText(registrationRequest.getPassword()) || registrationRequest.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password too short");
        }

        // sanitize user-provided fields
        String safeName = xssSanitizer.sanitize(registrationRequest.getName().trim());
        String safeEmail = registrationRequest.getEmail().trim().toLowerCase();

        UserRole role = UserRole.USER;

        // Only allow role override to ADMIN if the requester is already admin — otherwise ignore admin set
        if (registrationRequest.getRole() != null && registrationRequest.getRole().equalsIgnoreCase("admin")) {
            // check if current authenticated user is admin (self-promotion prevention)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                try {
                    User current = getLoginUser();
                    if (current != null && current.getRole() == UserRole.ADMIN) {
                        role = UserRole.ADMIN;
                    } // else keep USER role and ignore requested admin
                } catch (Exception e) {
                    // ignore and keep USER role
                }
            }
        }

        // ensure email uniqueness
        if (userRepo.findByEmail(safeEmail).isPresent()) {
            throw new ValidationException("Email already registered");
        }

        User user = User.builder()
                .name(safeName)
                .email(safeEmail)
                .password(passwordEncoder.encode(registrationRequest.getPassword()))
                .phoneNumber(registrationRequest.getPhoneNumber() != null ? xssSanitizer.sanitize(registrationRequest.getPhoneNumber()) : null)
                .role(role)
                .build();

        User savedUser = userRepo.save(user);
        log.debug("Registered user id: {}", savedUser.getId());

        UserDto userDto = entityDtoMapper.mapUserToDtoBasic(savedUser);
        return Response.builder()
                .status(200)
                .message("User Successfully Added")
                .user(userDto)
                .build();
    }

    @Override
    public Response getAllUsers() {

        List<User> users = userRepo.findAll();
        List<UserDto> userDtos = users.stream()
                .map(entityDtoMapper::mapUserToDtoBasic)
                .toList();

        return Response.builder()
                .status(200)
                .userList(userDtos)
                .build();
    }

    @Override
    public User getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new UsernameNotFoundException("User Not found");
        }
        String email = authentication.getName();
        log.info("User Email is: " + email);
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User Not found"));
    }

    @Override
    public Response loginUser(LoginRequest loginRequest) {
        if (loginRequest == null || !StringUtils.hasText(loginRequest.getEmail()) || !StringUtils.hasText(loginRequest.getPassword())) {
            throw new ValidationException("Email and password required");
        }

        User user = userRepo.findByEmail(loginRequest.getEmail().trim().toLowerCase()).orElseThrow(() -> new NotFoundException("Email not found"));
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Password does not match");
        }
        String token = jwtUtils.generateToken(user);

        return Response.builder()
                .status(200)
                .message("User Successfully Logged In")
                .token(token)
                .expirationTime("6 Month")
                .role(user.getRole().name())
                .build();
    }

    @Override
    public Response getUserInfoAndOrderHistory() {

        User user = getLoginUser();
        UserDto userDto = entityDtoMapper.mapUserToDtoPlusAddressAndOrderHistory(user);

        return Response.builder()
                .status(200)
                .user(userDto)
                .build();

    }
}
