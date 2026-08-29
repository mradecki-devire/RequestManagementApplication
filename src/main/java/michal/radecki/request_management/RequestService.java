package michal.radecki.request_management;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;

    Integer createRequest(@Valid CreateRequest request) {
        RequestEntity entity = new RequestEntity(request.name(), request.body(), RequestState.CREATED);
        entity = requestRepository.save(entity);
        return entity.getId();
    }
}
