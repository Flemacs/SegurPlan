package com.segurplan.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI 3 de la API de SEGURPLAN.
 *
 * Swagger UI:   /swagger-ui.html
 * Definición:   /v3/api-docs  (JSON)   y   /v3/api-docs.yaml
 *
 * Se desactiva con SWAGGER_ENABLED=false (recomendado en producción).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI segurplanOpenApi() {

        String guia =
                "API REST de la plataforma de seguros y AFP **SEGURPLAN**.\n"
                + "\n"
                + "### Cómo probar los endpoints protegidos\n"
                + "1. Ejecute `GET /api/csrf` y copie el valor de `token`.\n"
                + "2. Ejecute `POST /api/usuarios/login` con un usuario de prueba (el navegador guarda la cookie `JSESSIONID`).\n"
                + "3. Pulse **Authorize**, pegue el token en `csrf` y confirme.\n"
                + "4. Las operaciones `POST`, `PUT` y `DELETE` envían el token en la cabecera `X-CSRF-TOKEN`; las `GET` no lo necesitan.\n"
                + "5. Si cierra sesión o vuelve a iniciarla, pida un token CSRF nuevo.\n"
                + "\n"
                + "### Roles\n"
                + "`Cliente`, `Asesor` y `Administrador`. Cada operación indica en su descripción qué roles pueden usarla. Una respuesta **401** significa que no hay sesión; **403**, que el rol no alcanza o que el registro pertenece a otro usuario.\n"
                + "\n";

        return new OpenAPI()
                .info(new Info()
                        .title("SEGURPLAN API")
                        .version("6.5.0")
                        .description(guia)
                        .contact(new Contact().name("Equipo SEGURPLAN")))
                .servers(List.of(new Server()
                        .url("/")
                        .description("Servidor actual")))
                .components(new Components()
                        .addSecuritySchemes("sesion", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")
                                .description("Cookie de sesión que crea "
                                        + "POST /api/usuarios/login. El "
                                        + "navegador la envía sola."))
                        .addSecuritySchemes("csrf", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-CSRF-TOKEN")
                                .description("Token de GET /api/csrf. "
                                        + "Obligatorio en POST, PUT y DELETE.")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("sesion")
                        .addList("csrf"));
    }

    /**
     * Agrega a cada operación protegida las respuestas 401 (sin sesión) y
     * 403 (rol insuficiente o registro ajeno), para que Swagger las muestre.
     */
    @Bean
    public OpenApiCustomizer respuestasComunes() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(item ->
                    item.readOperations().forEach(op -> {
                        boolean publica = op.getSecurity() != null
                                && op.getSecurity().isEmpty();
                        if (publica) {
                            return;
                        }
                        if (op.getResponses() == null) {
                            op.setResponses(new ApiResponses());
                        }
                        op.getResponses().addApiResponse("401",
                                new ApiResponse().description(
                                        "No hay sesión iniciada."));
                        op.getResponses().addApiResponse("403",
                                new ApiResponse().description(
                                        "El rol no alcanza o el registro "
                                        + "pertenece a otro usuario."));
                    }));
        };
    }
}
