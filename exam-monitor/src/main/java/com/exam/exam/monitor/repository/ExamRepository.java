package com.exam.exam.monitor.repository;

import com.exam.exam.monitor.model.Exam;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExamRepository extends MongoRepository<Exam, String> {

}
