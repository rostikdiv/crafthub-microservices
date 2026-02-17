package com.crafthub.user_service.controller;

import com.crafthub.user_service.dto.address.SellerPointDTO;
import com.crafthub.user_service.service.SellerPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sellers/points")
@RequiredArgsConstructor
public class SellerPointController {

    private final SellerPointService pointService;

    // Створити точку
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerPointDTO> createPoint(@RequestBody SellerPointDTO dto) {
        return ResponseEntity.ok(pointService.createPoint(getCurrentUserId(), dto));
    }

    // Отримати мої точки
    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<SellerPointDTO>> getMyPoints() {
        return ResponseEntity.ok(pointService.getMyPoints(getCurrentUserId()));
    }

    // Оновити точку
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerPointDTO> updatePoint(@PathVariable UUID id, @RequestBody SellerPointDTO dto) {
        return ResponseEntity.ok(pointService.updatePoint(getCurrentUserId(), id, dto));
    }

    // Видалити точку
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> deletePoint(@PathVariable UUID id) {
        pointService.deletePoint(getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    private UUID getCurrentUserId() {
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(userIdStr);
    }
}
