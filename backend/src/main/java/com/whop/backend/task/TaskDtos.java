package com.whop.backend.task;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class TaskDtos {
    private TaskDtos() {}

    public record CreateTaskRequest(
            @NotBlank @Size(min = 3, max = 120) String title,
            @NotBlank @Size(min = 10, max = 5000) String description,
            @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2)
                    BigDecimal budgetAmount,
            @NotBlank @Pattern(regexp = "^USD$") String budgetCurrency) {}

    public record TaskOwnerResponse(UUID id, String username) {}

    public record AcceptedOfferSummary(UUID id, BigDecimal amount, String currency, TaskOwnerResponse seller) {}

    public record TaskResponse(
            UUID id,
            String title,
            String description,
            BigDecimal budgetAmount,
            String budgetCurrency,
            TaskStatus status,
            OffsetDateTime createdAt,
            TaskOwnerResponse owner,
            AcceptedOfferSummary acceptedOffer) {
        public static TaskResponse from(TaskEntity task) {
            AcceptedOfferSummary acceptedOfferSummary = null;
            if (task.getAcceptedOffer() != null) {
                acceptedOfferSummary =
                        new AcceptedOfferSummary(
                                task.getAcceptedOffer().getId(),
                                task.getAcceptedOffer().getAmount(),
                                task.getAcceptedOffer().getCurrency(),
                                new TaskOwnerResponse(
                                        task.getAcceptedOffer().getSeller().getId(),
                                        task.getAcceptedOffer().getSeller().getUsername()));
            }
            return new TaskResponse(
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getBudgetAmount(),
                    task.getBudgetCurrency(),
                    task.getStatus(),
                    task.getCreatedAt(),
                    new TaskOwnerResponse(
                            task.getOwner().getId(),
                            task.getOwner().getUsername()),
                    acceptedOfferSummary);
        }
    }
}
