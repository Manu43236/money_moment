package com.moneymoment.lending.common.spec;

import com.moneymoment.lending.entities.EmiScheduleEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class EmiScheduleSpecification {

    private EmiScheduleSpecification() {}

    public static Specification<EmiScheduleEntity> hasStatus(String status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<EmiScheduleEntity> dueDateFrom(LocalDate from) {
        return (root, query, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.get("dueDate"), from);
    }

    public static Specification<EmiScheduleEntity> dueDateTo(LocalDate to) {
        return (root, query, cb) ->
                to == null ? null : cb.lessThanOrEqualTo(root.get("dueDate"), to);
    }

    public static Specification<EmiScheduleEntity> hasLoanNumber(String loanNumber) {
        return (root, query, cb) ->
                loanNumber == null ? null : cb.equal(root.get("loan").get("loanNumber"), loanNumber);
    }

    public static Specification<EmiScheduleEntity> dpdFrom(Integer min) {
        return (root, query, cb) ->
                min == null ? null : cb.greaterThanOrEqualTo(root.get("daysPastDue"), min);
    }

    public static Specification<EmiScheduleEntity> dpdTo(Integer max) {
        return (root, query, cb) ->
                max == null ? null : cb.lessThanOrEqualTo(root.get("daysPastDue"), max);
    }
}
