package com.moneymoment.lending.common.spec;

import com.moneymoment.lending.entities.CustomerEntity;
import org.springframework.data.jpa.domain.Specification;

public class CustomerSpecification {

    private CustomerSpecification() {}

    public static Specification<CustomerEntity> nameLike(String name) {
        return (root, query, cb) ->
                name == null ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<CustomerEntity> hasEmploymentType(String employmentType) {
        return (root, query, cb) ->
                employmentType == null ? null : cb.equal(root.get("employmentType").as(String.class), employmentType);
    }

    public static Specification<CustomerEntity> emailLike(String email) {
        return (root, query, cb) ->
                email == null ? null : cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
    }

    public static Specification<CustomerEntity> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }
}
