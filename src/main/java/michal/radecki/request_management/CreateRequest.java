package michal.radecki.request_management;

import jakarta.validation.constraints.NotBlank;

public record CreateRequest(@NotBlank String name, @NotBlank String body) {}
