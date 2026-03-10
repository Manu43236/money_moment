package com.moneymoment.lending.repos;

import com.moneymoment.lending.entities.EmiScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmiScheduleRepository extends JpaRepository<EmiScheduleEntity, Long>, JpaSpecificationExecutor<EmiScheduleEntity> {

    List<EmiScheduleEntity> findByLoanIdOrderByEmiNumberAsc(Long loanId);

    Optional<EmiScheduleEntity> findByLoanIdAndEmiNumber(Long loanId, Integer emiNumber);

    List<EmiScheduleEntity> findByStatus(String status);

    List<EmiScheduleEntity> findByDueDateBetween(LocalDate startDate, LocalDate endDate);

    List<EmiScheduleEntity> findByDueDateAndStatus(LocalDate dueDate, String status);

    Long countByLoanIdAndStatus(Long loanId, String status);

    // Batch EOD: load all EMIs for active loans in one query with JOIN FETCH
    @Query("SELECT e FROM EmiScheduleEntity e JOIN FETCH e.loan l JOIN FETCH l.loanStatus WHERE l.loanStatus.code IN :codes")
    List<EmiScheduleEntity> findEmisByLoanStatusCodes(@Param("codes") List<String> codes);
}