package michal.radecki.request_management.dto;

import michal.radecki.request_management.domain.RequestState;

import java.time.LocalDateTime;

public record RequestStateHistoryDto(Integer id,
                                     Integer requestId,
                                     String name,
                                     String body,
                                     String reason,
                                     String publicationIdentifier,
                                     RequestState state,
                                     LocalDateTime changedAt) {
}
