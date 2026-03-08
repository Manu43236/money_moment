package com.moneymoment.lending.repos;

import com.moneymoment.lending.entities.EodLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EodLogRepository extends JpaRepository<EodLogEntity, Long> {
    Page<EodLogEntity> findAllByOrderByRunDateDesc(Pageable pageable);
}
