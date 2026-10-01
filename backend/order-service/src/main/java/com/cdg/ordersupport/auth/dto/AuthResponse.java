package com.cdg.ordersupport.auth.dto;
import com.cdg.ordersupport.user.Role;
public record AuthResponse(String accessToken, Role role, String displayName) {}
