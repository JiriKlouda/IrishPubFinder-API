package com.irishpubfinder.api.repository;

import com.irishpubfinder.api.model.ApiCallLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface ApiCallLogRepository extends JpaRepository<ApiCallLog, Long> {

    long countByCallTypeAndCalledAtBetween(String callType, Instant from, Instant to);
}
