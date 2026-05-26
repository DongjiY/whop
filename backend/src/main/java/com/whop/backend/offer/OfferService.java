package com.whop.backend.offer;

import com.whop.backend.auth.AuthUserPrincipal;
import com.whop.backend.auth.UserEntity;
import com.whop.backend.auth.UserRepository;
import com.whop.backend.offer.OfferDtos.CreateOfferRequest;
import com.whop.backend.offer.OfferDtos.OfferResponse;
import com.whop.backend.task.TaskEntity;
import com.whop.backend.task.TaskNotFoundException;
import com.whop.backend.task.TaskRepository;
import com.whop.backend.task.TaskStatus;
import com.whop.backend.transaction.TransactionEntity;
import com.whop.backend.transaction.TransactionRepository;
import com.whop.backend.transaction.TransactionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfferService {
    private final OfferRepository offerRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public OfferService(
            OfferRepository offerRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository) {
        this.offerRepository = offerRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public OfferResponse createOffer(
            UUID taskId, CreateOfferRequest request, Authentication authentication) {
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        TaskEntity task = findTask(taskId);
        UserEntity seller = findUser(principal.getId());

        if (task.getStatus() != TaskStatus.OPEN) {
            throw new InvalidOfferStateException();
        }

        if (task.getOwner().getId().equals(seller.getId())) {
            throw new ForbiddenOfferActionException();
        }

        if (offerRepository.existsByTaskIdAndSellerId(taskId, seller.getId())) {
            throw new DuplicateOfferException();
        }

        OfferEntity offer = new OfferEntity();
        offer.setTask(task);
        offer.setSeller(seller);
        offer.setAmount(request.amount());
        offer.setCurrency(request.currency());
        offer.setMessage(request.message().trim());
        offer.setStatus(OfferStatus.PENDING);

        return OfferResponse.from(offerRepository.save(offer));
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> listOffers(UUID taskId, Authentication authentication) {
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        TaskEntity task = findTask(taskId);

        List<OfferEntity> offers =
                task.getOwner().getId().equals(principal.getId())
                        ? offerRepository.findByTaskIdOrderByCreatedAtDesc(taskId)
                        : offerRepository.findByTaskIdAndSellerIdOrderByCreatedAtDesc(
                                taskId, principal.getId());

        return offers.stream().map(OfferResponse::from).toList();
    }

    @Transactional
    public OfferResponse acceptOffer(UUID taskId, UUID offerId, Authentication authentication) {
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        TaskEntity task = findTask(taskId);

        if (!task.getOwner().getId().equals(principal.getId())) {
            throw new ForbiddenOfferActionException();
        }

        OfferEntity selected =
                offerRepository.findByIdAndTaskId(offerId, taskId).orElseThrow(OfferNotFoundException::new);

        if (selected.getStatus() != OfferStatus.PENDING) {
            throw new InvalidOfferStateException();
        }

        selected.setStatus(OfferStatus.ACCEPTED);
        offerRepository.save(selected);

        List<OfferEntity> others =
                offerRepository.findByTaskIdAndStatusAndIdNot(taskId, OfferStatus.PENDING, offerId);
        for (OfferEntity other : others) {
            other.setStatus(OfferStatus.REJECTED);
        }
        offerRepository.saveAll(others);

        task.setStatus(TaskStatus.ASSIGNED);
        task.setAcceptedOffer(selected);
        taskRepository.save(task);

        TransactionEntity transaction = new TransactionEntity();
        transaction.setTask(task);
        transaction.setOffer(selected);
        transaction.setBuyer(task.getOwner());
        transaction.setSeller(selected.getSeller());
        transaction.setAmount(selected.getAmount());
        transaction.setCurrency(selected.getCurrency());
        transaction.setStatus(TransactionStatus.RECORDED);
        transactionRepository.save(transaction);

        return OfferResponse.from(selected);
    }

    @Transactional
    public OfferResponse withdrawOffer(UUID taskId, UUID offerId, Authentication authentication) {
        AuthUserPrincipal principal = (AuthUserPrincipal) authentication.getPrincipal();
        findTask(taskId);

        OfferEntity offer =
                offerRepository.findByIdAndTaskId(offerId, taskId).orElseThrow(OfferNotFoundException::new);

        if (!offer.getSeller().getId().equals(principal.getId())) {
            throw new ForbiddenOfferActionException();
        }

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new InvalidOfferStateException();
        }

        offer.setStatus(OfferStatus.WITHDRAWN);
        return OfferResponse.from(offerRepository.save(offer));
    }

    private TaskEntity findTask(UUID taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private UserEntity findUser(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }
}
