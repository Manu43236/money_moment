package com.moneymoment.lending.documents.repos;

import com.moneymoment.lending.documents.entities.DocumentEntity;
import com.moneymoment.lending.loans.Entity.LoanEntity;

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