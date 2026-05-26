package com.whop.backend.offer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class OfferDtos {
    private OfferDtos() {}

    public record CreateOfferRequest(
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2)
                    BigDecimal amount,
            @NotBlank @Pattern(regexp = "^USD$") String currency,
            @NotBlank @Size(min = 5, max = 5000) String message) {}

    public record OfferSellerResponse(UUID id, String username) {}

    public record OfferResponse(
            UUID id,
            UUID taskId,
            BigDecimal amount,
            String currency,
            String message,
            OfferStatus status,
            OffsetDateTime createdAt,
            OfferSellerResponse seller) {
        public static OfferResponse from(OfferEntity offer) {
            return new OfferResponse(
                    offer.getId(),
                    offer.getTask().getId(),
                    offer.getAmount(),
                    offer.getCurrency(),
                    offer.getMessage(),
                    offer.getStatus(),
                    offer.getCreatedAt(),
                    new OfferSellerResponse(offer.getSeller().getId(), offer.getSeller().getUsername()));
        }
    }
}
