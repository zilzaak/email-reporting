package app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DIU Email SLA Reporting API")
                        .version("1.0")
                        .description("Read-only Gmail metadata API for DIU Email Reply & SLA Monitoring. "
                                + "No authentication required (internal service).")
                        .contact(new Contact()
                                .name("DIU IT Team")
                                .email("support@daffodilvarsity.edu.bd")
                                .url("https://daffodilvarsity.edu.bd"))
                        .license(new License()
                                .name("Internal Use Only")
                                .url("https://daffodilvarsity.edu.bd")));
    }
}