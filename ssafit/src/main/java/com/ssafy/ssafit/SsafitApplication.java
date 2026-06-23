package com.ssafy.ssafit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@MapperScan(
        basePackages = {
            "com.ssafy.ssafit.member.mapper",
            "com.ssafy.ssafit.music.mapper",
            "com.ssafy.ssafit.musicreview.mapper",
            "com.ssafy.ssafit.playlist.mapper",
            "com.ssafy.ssafit.recommendation.mapper",
            "com.ssafy.ssafit.runningprofile.mapper",
            "com.ssafy.ssafit.runningrecord.mapper",
            "com.ssafy.ssafit.runningsession.mapper",
            "com.ssafy.ssafit.spotify.mapper"
        })
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
