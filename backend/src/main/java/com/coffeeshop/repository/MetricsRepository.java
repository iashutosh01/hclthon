package com.coffeeshop.repository;

import com.coffeeshop.model.Metrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface MetricsRepository extends JpaRepository<Metrics, Long> {

    List<Metrics> findTop10ByOrderByRecordedAtDesc();

    List<Metrics> findByRecordedAtBetween(Instant start, Instant end);
}
