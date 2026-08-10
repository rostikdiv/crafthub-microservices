package com.milhub.delivery_service.dto.location;

import java.util.UUID;

/**
 * Data Transfer Object for branch response details.
 */
public record BranchResponseDTO(
                UUID id,
                String externalId,
                String branchNumber, // Branch index or number
                String name // Full branch name/address
) {
}