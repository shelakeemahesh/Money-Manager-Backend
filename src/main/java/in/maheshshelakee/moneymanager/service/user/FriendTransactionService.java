package in.maheshshelakee.moneymanager.service.user;

import in.maheshshelakee.moneymanager.dto.*;
import in.maheshshelakee.moneymanager.entity.*;
import in.maheshshelakee.moneymanager.exception.ResourceNotFoundException;
import in.maheshshelakee.moneymanager.repository.FriendTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendTransactionService {

    private final FriendTransactionRepository friendTransactionRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<FriendTransactionResponseDTO> getAllByUser(
            String email,
            FriendTransactionType type,
            FriendTransactionStatus status,
            String friendName) {
        User user = userService.getUserByEmail(email);
        return friendTransactionRepository.findFilteredTransactions(user, type, status, friendName)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FriendTransactionResponseDTO getById(Long id, String email) {
        User user = userService.getUserByEmail(email);
        FriendTransaction transaction = friendTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FriendTransaction", id));
        if (!isOwnerOrShared(transaction.getUser(), user)) {
            throw new ResourceNotFoundException("FriendTransaction", id);
        }
        return toResponseDTO(transaction);
    }

    @Transactional
    public FriendTransactionResponseDTO createTransaction(FriendTransactionRequestDTO dto, String email) {
        User user = userService.getUserByEmail(email);
        FriendTransaction transaction = FriendTransaction.builder()
                .friendName(dto.getFriendName().trim())
                .type(dto.getType())
                .amount(dto.getAmount())
                .description(dto.getDescription())
                .transactionDate(dto.getTransactionDate())
                .dueDate(dto.getDueDate())
                .status(FriendTransactionStatus.PENDING)
                .user(user)
                .build();
        return toResponseDTO(friendTransactionRepository.save(transaction));
    }

    @Transactional
    public FriendTransactionResponseDTO updateTransaction(Long id, FriendTransactionRequestDTO dto, String email) {
        User user = userService.getUserByEmail(email);
        FriendTransaction transaction = friendTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FriendTransaction", id));
        if (!isOwnerOrShared(transaction.getUser(), user)) {
            throw new ResourceNotFoundException("FriendTransaction", id);
        }

        transaction.setFriendName(dto.getFriendName().trim());
        transaction.setType(dto.getType());
        transaction.setAmount(dto.getAmount());
        transaction.setDescription(dto.getDescription());
        transaction.setTransactionDate(dto.getTransactionDate());
        transaction.setDueDate(dto.getDueDate());

        return toResponseDTO(friendTransactionRepository.save(transaction));
    }

    @Transactional
    public void deleteTransaction(Long id, String email) {
        User user = userService.getUserByEmail(email);
        FriendTransaction transaction = friendTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FriendTransaction", id));
        if (!isOwnerOrShared(transaction.getUser(), user)) {
            throw new ResourceNotFoundException("FriendTransaction", id);
        }
        friendTransactionRepository.delete(transaction);
    }

    @Transactional
    public FriendTransactionResponseDTO markAsSettled(Long id, String email) {
        User user = userService.getUserByEmail(email);
        FriendTransaction transaction = friendTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FriendTransaction", id));
        if (!isOwnerOrShared(transaction.getUser(), user)) {
            throw new ResourceNotFoundException("FriendTransaction", id);
        }

        transaction.setStatus(FriendTransactionStatus.SETTLED);
        return toResponseDTO(friendTransactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public FriendSummaryDTO getFriendSummary(String friendName, String email) {
        User user = userService.getUserByEmail(email);
        // Find all transactions for this user first
        List<FriendTransaction> allTxs = friendTransactionRepository.findFilteredTransactions(user, null, null, null);

        // Filter for specific friend name (case-insensitive)
        List<FriendTransaction> friendTxs = allTxs.stream()
                .filter(tx -> tx.getFriendName().equalsIgnoreCase(friendName.trim()))
                .collect(Collectors.toList());

        BigDecimal totalGiven = BigDecimal.ZERO;
        BigDecimal totalTaken = BigDecimal.ZERO;

        List<FriendTransactionResponseDTO> pendingTxs = new ArrayList<>();
        List<FriendTransactionResponseDTO> mappedAllTxs = new ArrayList<>();

        for (FriendTransaction tx : friendTxs) {
            FriendTransactionResponseDTO resp = toResponseDTO(tx);
            mappedAllTxs.add(resp);

            if (tx.getStatus() == FriendTransactionStatus.PENDING) {
                pendingTxs.add(resp);
                if (tx.getType() == FriendTransactionType.GIVEN) {
                    totalGiven = totalGiven.add(tx.getAmount());
                } else if (tx.getType() == FriendTransactionType.TAKEN) {
                    totalTaken = totalTaken.add(tx.getAmount());
                }
            }
        }

        return FriendSummaryDTO.builder()
                .friendName(friendName)
                .totalGiven(totalGiven)
                .totalTaken(totalTaken)
                .netBalance(totalGiven.subtract(totalTaken))
                .pendingTransactions(pendingTxs)
                .allTransactions(mappedAllTxs)
                .build();
    }

    @Transactional(readOnly = true)
    public OverallSummaryDTO getOverallSummary(String email) {
        User user = userService.getUserByEmail(email);
        List<FriendTransaction> allTxs = friendTransactionRepository.findFilteredTransactions(user, null, null, null);

        BigDecimal totalGiven = BigDecimal.ZERO;
        BigDecimal totalTaken = BigDecimal.ZERO;

        Map<String, BigDecimal[]> friendStatsMap = new HashMap<>(); // Key: Friend Name (normalized trim), Value: [Given, Taken]

        for (FriendTransaction tx : allTxs) {
            String fName = tx.getFriendName().trim();
            // We want to group by friendName. To preserve casing from first encountered, we can find key matching case-insensitively
            String existingKey = friendStatsMap.keySet().stream()
                    .filter(k -> k.equalsIgnoreCase(fName))
                    .findFirst()
                    .orElse(fName);

            BigDecimal[] stats = friendStatsMap.computeIfAbsent(existingKey, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});

            if (tx.getStatus() == FriendTransactionStatus.PENDING) {
                if (tx.getType() == FriendTransactionType.GIVEN) {
                    totalGiven = totalGiven.add(tx.getAmount());
                    stats[0] = stats[0].add(tx.getAmount());
                } else if (tx.getType() == FriendTransactionType.TAKEN) {
                    totalTaken = totalTaken.add(tx.getAmount());
                    stats[1] = stats[1].add(tx.getAmount());
                }
            }
        }

        List<OverallSummaryDTO.FriendBalanceItem> friendBalances = friendStatsMap.entrySet().stream()
                .map(entry -> {
                    BigDecimal given = entry.getValue()[0];
                    BigDecimal taken = entry.getValue()[1];
                    return OverallSummaryDTO.FriendBalanceItem.builder()
                            .friendName(entry.getKey())
                            .totalGiven(given)
                            .totalTaken(taken)
                            .netBalance(given.subtract(taken))
                            .build();
                })
                .collect(Collectors.toList());

        return OverallSummaryDTO.builder()
                .totalGiven(totalGiven)
                .totalTaken(totalTaken)
                .netBalance(totalGiven.subtract(totalTaken))
                .friendBalances(friendBalances)
                .build();
    }

    private FriendTransactionResponseDTO toResponseDTO(FriendTransaction entity) {
        return FriendTransactionResponseDTO.builder()
                .id(entity.getId())
                .friendName(entity.getFriendName())
                .type(entity.getType())
                .amount(entity.getAmount())
                .description(entity.getDescription())
                .transactionDate(entity.getTransactionDate())
                .dueDate(entity.getDueDate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private boolean isOwnerOrShared(User owner, User currentUser) {
        if (owner.getId().equals(currentUser.getId())) {
            return true;
        }
        String ownerEmail = owner.getEmail();
        String currentEmail = currentUser.getEmail();
        return (ownerEmail.equals("shelakemahesh024@gmail.com") || ownerEmail.equals("shelakemahesh91@gmail.com"))
                && (currentEmail.equals("shelakemahesh024@gmail.com") || currentEmail.equals("shelakemahesh91@gmail.com"));
    }
}
