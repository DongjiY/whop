package com.whop.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public class AuthDtos {
    private AuthDtos() {}

    public record SignupRequest(
            @NotBlank
                    @Size(min = 3, max = 30)
                    @Pattern(regexp = "^[a-z0-9_]+$")
                    String username,
            @NotBlank @Size(min = 8, max = 100) String password) {}

    public record LoginRequest(
            @NotBlank
                    @Size(min = 3, max = 30)
                    @Pattern(regexp = "^[a-z0-9_]+$")
                    String username,
            @NotBlank @Size(min = 8, max = 100) String password) {}

    public record MeResponse(UUID id, String username, OffsetDateTime createdAt) {}

    public record ApiError(String code, String message) {}
}
