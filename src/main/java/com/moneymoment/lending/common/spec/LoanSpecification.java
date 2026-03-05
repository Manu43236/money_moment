package com.moneymoment.lending.common.spec;

import com.moneymoment.lending.entities.LoanEntity;
import org.springframework.data.jpa.domain.Specification;

public class LoanSpecification {

    private LoanSpecification() {}

    public static Specification<LoanEntity> hasStatus(String status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("loanStatus").get("code"), status);
    }

    public static Specification<LoanEntity> hasCustomerId(Long customerId) {
        return (root, query, cb) ->
                customerId == null ? null : cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<LoanEntity> hasLoanTypeCode(String loanTypeCode) {
        return (root, query, cb) ->
                loanTypeCode == null ? null : cb.equal(root.get("loanType").get("code"), loanTypeCode);
    }
}
