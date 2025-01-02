package com.settlementsystem.settlement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.settlementsystem.settlement.entity.Settlement;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
}
