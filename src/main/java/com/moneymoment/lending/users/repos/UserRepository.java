package com.moneymoment.lending.users.repos;

import com.moneymoment.lending.users.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
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
}