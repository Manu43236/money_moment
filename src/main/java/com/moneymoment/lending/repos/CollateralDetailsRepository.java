package com.moneymoment.lending.repos;

import com.moneymoment.lending.entities.CollateralDetailsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CollateralDetailsRepository extends JpaRepository<CollateralDetailsEntity, Long> {

    Optional<CollateralDetailsEntity> findByCollateralNumber(String collateralNumber);

    Optional<CollateralDetailsEntity> findByLoanId(Long loanId);

    List<CollateralDetailsEntity> findByCustomerId(Long customerId);

    List<CollateralDetailsEntity> findByCollateralStatus(String collateralStatus);

    List<CollateralDetailsEntity> findByCollateralType(String collateralType);
}