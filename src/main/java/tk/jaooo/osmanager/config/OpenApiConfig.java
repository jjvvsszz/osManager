package tk.jaooo.osmanager.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "OS Manager API",
                version = "1.0",
                description = "API para gerenciamento de Ordens de Serviço e integração com Demandanet/OCI",
                contact = @Contact(name = "Suporte", email = "suporte@jaooo.tk")
        ),
        // Aplica a segurança globalmente em todos os endpoints
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Insira o token JWT obtido no endpoint /api/authenticate"
)
public class OpenApiConfig {
}
