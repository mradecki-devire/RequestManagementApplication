package michal.radecki.request_management.repository;

import michal.radecki.request_management.entity.RequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestRepository extends JpaRepository<RequestEntity, Integer> {
}
