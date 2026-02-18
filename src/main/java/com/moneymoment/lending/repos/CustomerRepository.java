package com.moneymoment.lending.repos;

import org.springframework.stereotype.Repository;

import com.moneymoment.lending.entities.CustomerEntity;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

    Optional<CustomerEntity> findByEmail(String email);

    Optional<CustomerEntity> findByCustomerNumber(String customerNumber);

    

    
}