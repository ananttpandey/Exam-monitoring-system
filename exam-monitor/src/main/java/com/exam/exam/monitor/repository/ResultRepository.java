package com.exam.exam.monitor.repository;

import com.exam.exam.monitor.model.Result;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ResultRepository extends MongoRepository<Result, String> {
    List<Result> findByUserEmail(String userEmail);
}
