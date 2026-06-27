package com.iduenduen.mtsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class IDuenDuenApplication {

	public static void main(String[] args) {
		// LocalDateTime.now()/LocalDate.now()는 JVM 기본 타임존을 따른다. 배포 서버(UTC)와 로컬(KST)이
		// 달라 캔들 시각이 9시간 어긋나는 문제를 막기 위해, 빈/스케줄러 기동 전에 KST로 고정한다.
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		SpringApplication.run(IDuenDuenApplication.class, args);
	}

}
