package com.milhub.delivery_service.dto.request;

/**
 * Data Transfer Object for creating a new branch.
 */
public record BranchCreateDTO(
                String externalId, // Carrier-specific reference
                String branchNumber, // Branch index
                String name // Full branch name
) {
}