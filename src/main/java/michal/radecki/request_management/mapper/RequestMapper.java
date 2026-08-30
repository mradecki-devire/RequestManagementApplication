package michal.radecki.request_management.mapper;

import michal.radecki.request_management.domain.RequestState;
import michal.radecki.request_management.dto.RequestDto;
import michal.radecki.request_management.dto.RequestStateHistoryDto;
import michal.radecki.request_management.entity.RequestEntity;
import michal.radecki.request_management.entity.RequestStateHistoryEntity;
import michal.radecki.request_management.request.CreateRequest;

public class RequestMapper {

    public static RequestDto toDto(RequestEntity e) {
        return new RequestDto(e.getRequestId(), e.getState(), e.getName(), e.getBody(), e.getReason(),
                e.getPublicationIdentifier());
    }

    public static RequestStateHistoryDto toDto(RequestStateHistoryEntity e) {
        return new RequestStateHistoryDto(e.getId(), e.getRequestId(), e.getName(), e.getBody(), e.getReason(),
                e.getPublicationIdentifier(), e.getState(), e.getChangedAt());
    }

    public static RequestEntity toEntity(CreateRequest request) {
        return new RequestEntity(request.name(), request.body(), RequestState.CREATED);
    }
}
