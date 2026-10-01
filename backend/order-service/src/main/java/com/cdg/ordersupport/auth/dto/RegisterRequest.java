package com.cdg.ordersupport.auth.dto;
import jakarta.validation.constraints.*;
public record RegisterRequest(@Email @NotBlank String email, @NotBlank @Size(min=8) String password, @NotBlank @Size(max=120) String displayName) {}
