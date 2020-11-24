package com.mibs.asterisk.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@PropertySource("classpath:application.yml")
public class AppConfig {

	@Value("${app.user}")
	private String user;

	@Value("${app.host}")
	private String host;

	@Value("${app.password}")
	private String password;

	@Value("${app.port}")
	private int port;

}
