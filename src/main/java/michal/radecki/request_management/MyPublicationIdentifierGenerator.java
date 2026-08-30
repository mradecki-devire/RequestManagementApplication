package michal.radecki.request_management;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.UUID;

@Component("myPublicationIdentifierGenerator")
@RequiredArgsConstructor
public class MyPublicationIdentifierGenerator implements PublicationIdentifierGenerator {

    @Override
    public String generate() {
        UUID uuid = UUID.randomUUID();

        return new BigInteger(
                uuid.toString().replace("-", ""),
                16
        ).toString();
    }
}
