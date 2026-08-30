package michal.radecki.request_management.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateBodyRequest(@NotBlank String body) {}
