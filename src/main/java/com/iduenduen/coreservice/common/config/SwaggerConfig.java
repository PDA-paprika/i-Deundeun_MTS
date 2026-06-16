package com.iduenduen.coreservice.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openAPI() {
		SecurityScheme bearerScheme = new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT")
				.name("Authorization");

		return new OpenAPI()
				.addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
				.components(new Components().addSecuritySchemes("BearerAuth", bearerScheme))
				.info(new Info()
						.title("IDeundeun core-service Backend API")
						.description("프로디지털아카데미 아이 든든 프로젝트의 백엔드 API 문서입니다.")
						.version("1.0.0")
						.contact(new Contact()
								.name("IDeundeun Dev Team")
								.email("team.IDeundeun@example.com"))
						.license(new License()
								.name("Apache License 2.0")
								.url("https://www.apache.org/licenses/LICENSE-2.0.html")));
	}
}
