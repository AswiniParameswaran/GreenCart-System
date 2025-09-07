package com.example.shopBackend.service;

import com.example.shopBackend.dto.LoginRequest;
import com.example.shopBackend.dto.UserDto;
import com.example.shopBackend.entity.User;
import com.example.shopBackend.dto.Response;
import com.example.shopBackend.entity.User;

public interface UserService {
    Response registerUser(UserDto registrationRequest);
  Response loginUser(LoginRequest loginRequest);
    Response getAllUsers();
    User getLoginUser();
    Response getUserInfoAndOrderHistory();
}
