package com.exam.exam.monitor.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.exam.exam.monitor.model.User;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);


        User findByEmailAndPassword(String email, String password);
    }


