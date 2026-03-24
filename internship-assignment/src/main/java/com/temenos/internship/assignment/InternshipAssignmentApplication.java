package com.temenos.internship.assignment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InternshipAssignmentApplication {

	public static void main(String[] args) {
		SpringApplication.run(InternshipAssignmentApplication.class, args);
	}

}
