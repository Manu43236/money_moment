package com.moneymoment.lending.users.repos;

import com.moneymoment.lending.users.entities.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByRoleCode(String roleCode);

    List<RoleEntity> findByCanApprove(Boolean canApprove);

    List<RoleEntity> findByApprovalLevelGreaterThan(Integer level);

    List<RoleEntity> findByIsActive(Boolean isActive);
}