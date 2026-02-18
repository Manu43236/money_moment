package com.moneymoment.lending.repos;

import com.moneymoment.lending.entities.DocumentEntity;
import com.moneymoment.lending.entities.LoanEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    Optional<DocumentEntity> findByDocumentNumber(String documentNumber);

    List<DocumentEntity> findByCustomerId(Long customerId);

    List<DocumentEntity> findByLoanId(Long loanId);

    Optional<LoanEntity> findByCustomer_CustomerNumber(String customerNumber);
}