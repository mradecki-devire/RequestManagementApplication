package michal.radecki.request_management.dto;

import michal.radecki.request_management.domain.RequestState;

public record RequestDto(Integer requestId,
                         RequestState state,
                         String name,
                         String body,
                         String reason,
                         String publicationIdentifier) {
}
