package michal.radecki.request_management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import michal.radecki.request_management.RequestState;

@Entity
@Table(name = "requests")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private RequestState state;
    private String name;
    private String body;
    private String reason;
    @Column(name = "publication_identifier", unique = true)
    private String publicationIdentifier;
    @Version
    private Long version;

    public RequestEntity(String name, String body, RequestState state) {
        this.name = name;
        this.body = body;
        this.state = state;
    }
}
