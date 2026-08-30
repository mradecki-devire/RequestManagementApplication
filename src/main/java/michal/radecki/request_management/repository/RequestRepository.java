package michal.radecki.request_management.repository;

import michal.radecki.request_management.domain.RequestState;
import michal.radecki.request_management.entity.RequestEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestRepository extends JpaRepository<RequestEntity, Integer> {
    Page<RequestEntity> findByNameAndState(String name, RequestState state, Pageable pageable);
    Page<RequestEntity> findByName(String name, Pageable pageable);
    Page<RequestEntity> findByState(RequestState state, Pageable pageable);
}
