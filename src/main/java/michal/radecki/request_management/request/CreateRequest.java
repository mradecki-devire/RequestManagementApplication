package michal.radecki.request_management.request;

import jakarta.validation.constraints.NotBlank;

public record CreateRequest(@NotBlank String name,
                            @NotBlank String body) {}
