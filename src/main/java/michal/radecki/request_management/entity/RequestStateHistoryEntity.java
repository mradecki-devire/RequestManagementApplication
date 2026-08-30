package michal.radecki.request_management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import michal.radecki.request_management.domain.RequestState;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_state_history")
@Getter
@NoArgsConstructor
public class RequestStateHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer requestId;
    private String name;
    private String body;
    private String reason;
    @Column(name = "publication_identifier")
    private String publicationIdentifier;
    @Enumerated(EnumType.STRING)
    private RequestState state;
    private LocalDateTime changedAt;

    public RequestStateHistoryEntity(RequestEntity requestEntity) {
        this.requestId = requestEntity.getRequestId();
        this.name = requestEntity.getName();
        this.body = requestEntity.getBody();
        this.reason = requestEntity.getReason();
        this.publicationIdentifier = requestEntity.getPublicationIdentifier();
        this.state = requestEntity.getState();
        this.changedAt = LocalDateTime.now();
    }

}
