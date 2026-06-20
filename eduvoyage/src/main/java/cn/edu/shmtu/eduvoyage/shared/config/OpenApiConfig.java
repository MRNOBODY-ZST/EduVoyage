package cn.edu.shmtu.eduvoyage.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 3 definition. Module/role grouping is configured under
 * {@code springdoc.group-configs} in {@code application.yml}; here we declare
 * the global metadata and the JWT bearer security scheme so the "Authorize"
 * button works in Swagger UI.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI eduVoyageOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EduVoyage API")
                        .description("EduVoyage 在线学习平台 接口文档（响应式 / WebFlux）")
                        .version("v0.0.1")
                        .contact(new Contact().name("EduVoyage Team").email("dev@shmtu.edu.cn"))
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("在此粘贴登录接口返回的 Access Token")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
