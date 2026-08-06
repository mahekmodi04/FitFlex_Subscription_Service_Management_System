package com.fit.subscription.repository;

import com.fit.subscription.entity.DunningLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DunningLogRepository extends JpaRepository<DunningLog, Long> {
}
