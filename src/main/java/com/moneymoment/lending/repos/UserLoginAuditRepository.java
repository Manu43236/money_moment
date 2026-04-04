package com.moneymoment.lending.repos;

import com.moneymoment.lending.entities.UserLoginAuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserLoginAuditRepository extends JpaRepository<UserLoginAuditEntity, Long> {

    Page<UserLoginAuditEntity> findByUsername(String username, Pageable pageable);

    Optional<UserLoginAuditEntity> findTopByUsernameAndStatusOrderByLoginAtDesc(String username, String status);
}
