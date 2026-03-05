package com.moneymoment.lending.repos;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moneymoment.lending.entities.UserEntity;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByUserNumber(String userNumber);

    Optional<UserEntity> findByEmployeeId(String employeeId);

    Optional<UserEntity> findByEmail(String email);

    List<UserEntity> findByBranchCode(String branchCode);

    List<UserEntity> findByRegionCode(String regionCode);

    List<UserEntity> findByIsActive(Boolean isActive);

    Page<UserEntity> findAll(Pageable pageable);

    Page<UserEntity> findByBranchCode(String branchCode, Pageable pageable);

    Page<UserEntity> findByRoles_RoleCode(String roleCode, Pageable pageable);
}