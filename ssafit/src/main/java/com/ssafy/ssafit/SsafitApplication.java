package com.ssafy.ssafit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@MapperScan("com.ssafy.ssafit")
@SpringBootApplication
public class SsafitApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(SsafitApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(SsafitApplication.class);
	}

}
