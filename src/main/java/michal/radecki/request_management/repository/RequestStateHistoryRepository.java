package michal.radecki.request_management.repository;

import michal.radecki.request_management.entity.RequestStateHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestStateHistoryRepository extends JpaRepository<RequestStateHistoryEntity, Integer> {

    List<RequestStateHistoryEntity> findAllByRequestId(Integer requestId);
}
