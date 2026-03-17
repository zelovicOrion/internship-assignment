package com.temenos.internship.assignment.repository;

import com.temenos.internship.assignment.model.TimerEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface TimerRepository extends ReactiveCrudRepository<TimerEntity, UUID> {
}
