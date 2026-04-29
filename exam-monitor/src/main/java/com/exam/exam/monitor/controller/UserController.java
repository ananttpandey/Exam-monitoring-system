package com.exam.exam.monitor.controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.exam.exam.monitor.model.User;
import com.exam.exam.monitor.repository.UserRepository;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @PostMapping
    public User createUser(@RequestBody User user){
        return userRepository.save(user);
    }

    @GetMapping
    public List<User> getUsers(){
        return userRepository.findAll();
    }

    @PostMapping("/login")
    public User login(@RequestBody User user){
        return userRepository
                .findByEmailAndPassword(user.getEmail(), user.getPassword());
    }
}
