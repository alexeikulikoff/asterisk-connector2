package com.mibs.asterisk.web.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/change", "/add", "/remove", "/bridge", "/call", "/abandon", "/connect");
		config.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {

		List<String> allowedOriginPatterns = new ArrayList<>();
		allowedOriginPatterns.add("http://localhost:4800");
		String[] arr = allowedOriginPatterns.stream().toArray(String[]::new);

		registry.addEndpoint("/socket").setAllowedOrigins(arr).withSockJS();
	}

}
