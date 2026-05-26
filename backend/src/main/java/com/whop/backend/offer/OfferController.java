package com.whop.backend.offer;

import com.whop.backend.offer.OfferDtos.CreateOfferRequest;
import com.whop.backend.offer.OfferDtos.OfferResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/{taskId}/offers")
public class OfferController {
    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping
    public ResponseEntity<OfferResponse> createOffer(
            @PathVariable UUID taskId,
            @Valid @RequestBody CreateOfferRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(offerService.createOffer(taskId, request, authentication));
    }

    @GetMapping
    public ResponseEntity<List<OfferResponse>> listOffers(
            @PathVariable UUID taskId, Authentication authentication) {
        return ResponseEntity.ok(offerService.listOffers(taskId, authentication));
    }

    @PostMapping("/{offerId}/accept")
    public ResponseEntity<OfferResponse> acceptOffer(
            @PathVariable UUID taskId, @PathVariable UUID offerId, Authentication authentication) {
        return ResponseEntity.ok(offerService.acceptOffer(taskId, offerId, authentication));
    }

    @PostMapping("/{offerId}/withdraw")
    public ResponseEntity<OfferResponse> withdrawOffer(
            @PathVariable UUID taskId, @PathVariable UUID offerId, Authentication authentication) {
        return ResponseEntity.ok(offerService.withdrawOffer(taskId, offerId, authentication));
    }
}
