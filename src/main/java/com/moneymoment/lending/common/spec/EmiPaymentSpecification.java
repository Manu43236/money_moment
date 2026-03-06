package com.moneymoment.lending.common.spec;

import com.moneymoment.lending.entities.EmiPaymentEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class EmiPaymentSpecification {

    private EmiPaymentSpecification() {}

    public static Specification<EmiPaymentEntity> hasLoanNumber(String loanNumber) {
        return (root, query, cb) ->
                loanNumber == null ? null : cb.equal(root.get("loan").get("loanNumber"), loanNumber);
    }

    public static Specification<EmiPaymentEntity> hasPaymentStatus(String status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("paymentStatus"), status);
    }

    public static Specification<EmiPaymentEntity> hasPaymentMode(String mode) {
        return (root, query, cb) ->
                mode == null ? null : cb.equal(root.get("paymentMode"), mode);
    }

    public static Specification<EmiPaymentEntity> hasPaymentType(String type) {
        return (root, query, cb) ->
                type == null ? null : cb.equal(root.get("paymentType"), type);
    }

    public static Specification<EmiPaymentEntity> paymentDateFrom(LocalDate from) {
        return (root, query, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.<LocalDate>get("paymentDate"), from);
    }

    public static Specification<EmiPaymentEntity> paymentDateTo(LocalDate to) {
        return (root, query, cb) ->
                to == null ? null : cb.lessThanOrEqualTo(root.<LocalDate>get("paymentDate"), to);
    }
}
