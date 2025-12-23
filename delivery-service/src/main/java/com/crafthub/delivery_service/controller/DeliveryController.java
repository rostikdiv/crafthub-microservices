package com.crafthub.delivery_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/delivery")
public class DeliveryController {

    @GetMapping("/health")
    public String healthCheck() {
        return "Delivery Service is ready to ship!";
    }
}