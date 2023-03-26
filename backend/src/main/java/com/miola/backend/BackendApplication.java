package com.miola.backend;

import com.miola.backend.constant.FileConstant;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@SpringBootApplication
public class BackendApplication {

//	@Bean
//	public CorsFilter croCorsFilter() {
//		UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
//		CorsConfiguration corsConfiguration = new CorsConfiguration();
//		corsConfiguration.setAllowCredentials(true);
//		corsConfiguration.setAllowedOrigins(List.of("http://localhost:4200"));
//		corsConfiguration.setAllowedHeaders(Arrays.asList(
//				"Origin",
//				"Access-Control-Allow-Origin",
//				"Content-Type",
//				"Accept",
//				"Jwt-Token",
//				"Authorization",
//				"Origin, Accept",
//				"X-Requested-With",
//				"Access-Control-Request-Method",
//				"Access-Control-Request-Headers"));
//		corsConfiguration.setExposedHeaders(Arrays.asList(
//				"Origin",
//				"Content-Type",
//				"Accept",
//				"Jwt-Token",
//				"Authorization",
//				"Access-Control-Allow-Origin",
//				"Access-Control-Allow-Origin",
//				"Access-Control-Request-Headers",
//				"Access-Control-Allow-Credentials"));
//		corsConfiguration.setAllowedMethods(Arrays.asList(
//				"GET",
//				"POST",
//				"PUT",
//				"DELETE",
//				"OPTIONS"));
//		urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
//		return new CorsFilter(urlBasedCorsConfigurationSource);
//	}

	@Bean
	public CorsFilter corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);
		config.setAllowedOrigins(Collections.singletonList("http://localhost:4200"));  // TODO: lock down before deploying
		config.addAllowedHeader("*");
		config.addExposedHeader("*");
		config.addAllowedMethod("*");
		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
		new File(FileConstant.USER_FOLDER).mkdirs();
	}
}
