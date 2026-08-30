package michal.radecki.request_management;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public RequestEntity(String name, String body, RequestState state) {
        this.name = name;
        this.body = body;
        this.state = state;
    }
}
