package michal.radecki.request_management.dto;

import michal.radecki.request_management.RequestState;

public record RequestDto(Integer id,
                         RequestState state,
                         String name,
                         String body,
                         String reason,
                         String publicationIdentifier) {
}
