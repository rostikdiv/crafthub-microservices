package com.crafthub.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Main application class for the API Gateway.
 * Acts as the entry point for all external client requests, providing routing
 * and security.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableMethodSecurity
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}
}