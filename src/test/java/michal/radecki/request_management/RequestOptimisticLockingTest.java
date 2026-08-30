package michal.radecki.request_management;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import michal.radecki.request_management.entity.RequestEntity;
import michal.radecki.request_management.repository.RequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RequestOptimisticLockingTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private RequestRepository requestRepository;

    @Test
    void when_two_transactions_modify_same_request_then_second_update_should_fail() {
        // given
        RequestEntity request = new RequestEntity(
                "requestName",
                "requestBody",
                RequestState.CREATED
        );

        request = requestRepository.saveAndFlush(request);

        Integer requestId = request.getRequestId();

        EntityManager entityManager1 =
                entityManagerFactory.createEntityManager();

        EntityManager entityManager2 =
                entityManagerFactory.createEntityManager();

        try {
            RequestEntity request1 =
                    entityManager1.find(RequestEntity.class, requestId);

            RequestEntity request2 =
                    entityManager2.find(RequestEntity.class, requestId);

            // when - first transaction succeeds
            EntityTransaction transaction1 =
                    entityManager1.getTransaction();

            transaction1.begin();

            request1.setBody("updated by transaction 1");

            transaction1.commit();

            // then - second transaction tries to update stale entity
            EntityTransaction transaction2 =
                    entityManager2.getTransaction();

            transaction2.begin();

            request2.setBody("updated by transaction 2");

            assertThatThrownBy(transaction2::commit)
                    .isInstanceOfAny(
                            OptimisticLockException.class,
                            RollbackException.class
                    );
        } finally {
            entityManager1.close();
            entityManager2.close();
        }

        RequestEntity persisted =
                requestRepository.findById(requestId).orElseThrow();

        assertThat(persisted.getBody())
                .isEqualTo("updated by transaction 1");

        assertThat(persisted.getVersion())
                .isEqualTo(1L);
    }
}
