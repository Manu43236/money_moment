package com.moneymoment.lending.repos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.entities.LoanEntity;

@Repository
public interface LoanRepo extends JpaRepository<LoanEntity, Long>, JpaSpecificationExecutor<LoanEntity> {

    Optional<LoanEntity> findByLoanNumber(String loanNumber);

    List<LoanEntity> findByCustomerId(Long customerId);

    Page<LoanEntity> findByCustomerId(Long customerId, Pageable pageable);

    int countByCustomer_IdAndLoanStatus_CodeNotIn(Long customerId, List<String> excludedCodes);

    boolean existsByCustomer_IdAndLoanStatus_CodeIn(Long customerId, List<String> codes);

    // Batch EOD: load active loans with status eagerly
    @Query("SELECT l FROM LoanEntity l JOIN FETCH l.loanStatus WHERE l.loanStatus.code IN :codes")
    List<LoanEntity> findByLoanStatusCodes(@Param("codes") List<String> codes);
}
