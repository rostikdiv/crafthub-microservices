package com.crafthub.payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Main application class for the Payment Service.
 * This service manages financial transactions, including payments for orders.
 */
@SpringBootApplication
@EnableMethodSecurity
public class PaymentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentServiceApplication.class, args);
	}

}
