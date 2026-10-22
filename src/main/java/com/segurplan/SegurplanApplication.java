package com.segurplan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // habilita el respaldo automático diario (BackupService)
public class SegurplanApplication {

	public static void main(String[] args) {
		SpringApplication.run(SegurplanApplication.class, args);
	}

}
