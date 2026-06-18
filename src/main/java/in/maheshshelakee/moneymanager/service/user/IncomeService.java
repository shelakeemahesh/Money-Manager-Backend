package in.maheshshelakee.moneymanager.service.user;

import in.maheshshelakee.moneymanager.dto.IncomeDTO;
import in.maheshshelakee.moneymanager.entity.IncomeEntity;
import in.maheshshelakee.moneymanager.entity.User;
import in.maheshshelakee.moneymanager.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final IncomeRepository incomeRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<IncomeDTO> getAll(String email) {
        User user = userService.getUserByEmail(email);
        return incomeRepository.findByUserOrderByDateDesc(user)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public IncomeDTO add(IncomeDTO dto, String email) {
        User user = userService.getUserByEmail(email);
        IncomeEntity entity = IncomeEntity.builder()
                .source(dto.getSource().trim())
                .amount(dto.getAmount())
                .date(dto.getDate())
                .category(dto.getCategory())
                .icon(dto.getIcon() != null && !dto.getIcon().isBlank() ? dto.getIcon() : "💰")
                .user(user)
                .build();
        return toDTO(incomeRepository.save(entity));
    }

    @Transactional
    public IncomeDTO update(Long id, IncomeDTO dto, String email) {
        User user = userService.getUserByEmail(email);
        IncomeEntity entity = incomeRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Income record not found"));
        entity.setSource(dto.getSource().trim());
        entity.setAmount(dto.getAmount());
        entity.setDate(dto.getDate());
        entity.setCategory(dto.getCategory());
        if (dto.getIcon() != null && !dto.getIcon().isBlank()) {
            entity.setIcon(dto.getIcon());
        }
        return toDTO(incomeRepository.save(entity));
    }

    @Transactional
    public void delete(Long id, String email) {
        User user = userService.getUserByEmail(email);
        IncomeEntity entity = incomeRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Income record not found"));
        incomeRepository.delete(entity);
    }

    public IncomeDTO toDTO(IncomeEntity entity) {
        return IncomeDTO.builder()
                .id(entity.getId())
                .source(entity.getSource())
                .amount(entity.getAmount())
                .date(entity.getDate())
                .category(entity.getCategory())
                .icon(entity.getIcon())
                .build();
    }
}
