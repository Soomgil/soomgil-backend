package com.soomgil;

import org.springframework.boot.SpringApplication;

public class TestSoomgilBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(SoomgilBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
