package com.crafthub.delivery_service.repository;

import com.crafthub.delivery_service.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    // Знайти всі відділення у конкретному місті
    List<Branch> findByLocationId(UUID locationId);
}