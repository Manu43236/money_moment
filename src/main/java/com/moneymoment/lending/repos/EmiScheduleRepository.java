package com.moneymoment.lending.repos;

import com.moneymoment.lending.entities.EmiScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmiScheduleRepository extends JpaRepository<EmiScheduleEntity, Long> {

    List<EmiScheduleEntity> findByLoanIdOrderByEmiNumberAsc(Long loanId);

    Optional<EmiScheduleEntity> findByLoanIdAndEmiNumber(Long loanId, Integer emiNumber);

    List<EmiScheduleEntity> findByStatus(String status);

    List<EmiScheduleEntity> findByDueDateBetween(LocalDate startDate, LocalDate endDate);

    List<EmiScheduleEntity> findByDueDateAndStatus(LocalDate dueDate, String status);

    Long countByLoanIdAndStatus(Long loanId, String status);
}