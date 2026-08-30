package michal.radecki.request_management.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI requestManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Request Management API")
                        .description("""
                                REST API for managing requests through a defined lifecycle.

                                The API supports request creation, verification, acceptance,
                                rejection, publication and deletion. It also provides request
                                browsing with pagination and filtering, as well as a complete
                                audit history of request changes.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Michał Radecki")
                                .email("michal.radecki.dev@gmail.com")));
    }
}
