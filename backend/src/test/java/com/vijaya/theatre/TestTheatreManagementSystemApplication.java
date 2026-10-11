package com.vijaya.theatre;

import org.springframework.boot.SpringApplication;

public class TestTheatreManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.from(TheatreManagementSystemApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
