package michal.radecki.request_management.request;

import jakarta.validation.constraints.NotBlank;

public record RequestWithReason(@NotBlank String reason) {
}
