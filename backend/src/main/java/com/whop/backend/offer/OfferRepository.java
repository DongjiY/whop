package com.whop.backend.offer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<OfferEntity, UUID> {
    boolean existsByTaskIdAndSellerId(UUID taskId, UUID sellerId);

    List<OfferEntity> findByTaskIdOrderByCreatedAtDesc(UUID taskId);

    List<OfferEntity> findByTaskIdAndSellerIdOrderByCreatedAtDesc(UUID taskId, UUID sellerId);

    Optional<OfferEntity> findByIdAndTaskId(UUID id, UUID taskId);

    List<OfferEntity> findByTaskIdAndStatusAndIdNot(UUID taskId, OfferStatus status, UUID offerId);
}
