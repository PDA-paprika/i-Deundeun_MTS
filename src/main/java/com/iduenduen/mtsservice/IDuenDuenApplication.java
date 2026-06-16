package com.iduenduen.mtsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class IDuenDuenApplication {

	public static void main(String[] args) {
		SpringApplication.run(IDuenDuenApplication.class, args);
	}

}
