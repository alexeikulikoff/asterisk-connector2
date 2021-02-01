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

	@Value("${app.db_host}")
	private String dbHost;

	@Value("${app.db_user}")
	private String dbUser;

	@Value("${app.db_password}")
	private String dbPassword;

	@Value("${app.db_name}")
	private String dbName;

	@Value("${app.redis_host}")
	private String redisHost;

	@Value("${app.redis_user}")
	private String redisUser;

	@Value("${app.redis_pssword}")
	private String redisPssword;

	@Value("${app.redis_db}")
	private String redisDb;

	@Value("${app.rabbitmq_host}")
	private String rabbitmqHost;

	@Value("${app.rabbitmq_username}")
	private String rabbitmqUsername;

	@Value("${app.rabbitmq_password}")
	private String rabbitmqPassword;

}
