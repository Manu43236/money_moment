package com.moneymoment.lending.repos;

import com.moneymoment.lending.entities.EmiPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmiPaymentRepository extends JpaRepository<EmiPaymentEntity, Long>, JpaSpecificationExecutor<EmiPaymentEntity> {

    Optional<EmiPaymentEntity> findByPaymentNumber(String paymentNumber);

    List<EmiPaymentEntity> findByLoanIdOrderByPaymentDateDesc(Long loanId);

    List<EmiPaymentEntity> findByEmiScheduleId(Long emiScheduleId);

    List<EmiPaymentEntity> findByPaymentStatus(String paymentStatus);

    List<EmiPaymentEntity> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);

    List<EmiPaymentEntity> findByCustomerId(Long customerId);
}