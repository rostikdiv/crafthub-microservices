package com.crafthub.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@FeignClient(name = "user-service", path = "/api/v1/sellers")
public interface UserServiceClient {

    @PostMapping("/internal/{id}/sales/increment")
    void incrementSales(@PathVariable("id") UUID id);
}
