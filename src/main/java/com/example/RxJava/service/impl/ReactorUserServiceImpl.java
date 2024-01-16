package com.example.RxJava.service.impl;

import com.example.RxJava.controller.dto.UserIncomingDto;
import com.example.RxJava.controller.dto.UserOutgoingDto;
import com.example.RxJava.controller.mapper.UserMapper;
import com.example.RxJava.model.User;
import com.example.RxJava.repository.ReactorUserRepository;
import com.example.RxJava.service.ReactorUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ReactorUserServiceImpl implements ReactorUserService {
    private final ReactorUserRepository userRepository;
    private final UserMapper userMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public ReactorUserServiceImpl(ReactorUserRepository userRepository, UserMapper userMapper, KafkaTemplate<String, String> kafkaTemplate) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Flux<UserOutgoingDto> getAllUsers() {
        log.info("Getting all users");
        return userRepository.findAll()
                .map(userMapper::userToUserOutgoingDto)
                .doOnComplete(() -> kafkaTemplate.send("users-success", "Successfully retrieved all users"))
                .doOnError(error -> kafkaTemplate.send("users-error","Error retrieving all users"));
    }

    @Override
    public Mono<UserOutgoingDto> getUserById(Long id) {
        log.info("Getting user by id: {}", id);
        return userRepository.findById(id)
                .map(userMapper::userToUserOutgoingDto)
                .doOnSuccess(user -> kafkaTemplate.send("users-success", "Successfully retrieved user with id: " + id))
                .doOnError(error -> kafkaTemplate.send("users-error","Error retrieving user with id: " + id));
    }

    @Override
    public Mono<UserOutgoingDto> addUser(UserIncomingDto userIncomingDto) {
        log.info("Adding user: {}", userIncomingDto);
        User user = userMapper.userIncomingDtoToUser(userIncomingDto);
        return userRepository.save(user)
                .map(userMapper::userToUserOutgoingDto)
                .doOnSuccess(savedUser -> kafkaTemplate.send("users-success", "Successfully added user: " + savedUser))
                .doOnError(error -> kafkaTemplate.send("users-error","Error adding user: " + userIncomingDto));
    }

    @Override
    public Mono<UserOutgoingDto> updateUser(Long id, UserIncomingDto userIncomingDto) {
        log.info("Updating user with id: {}", id);
        User user = userMapper.userIncomingDtoToUser(userIncomingDto);
        return userRepository.findById(id)
                .flatMap(existingUser -> {
                    existingUser.setName(user.getName());
                    existingUser.setAge(user.getAge());
                    return userRepository.save(existingUser);
                })
                .map(userMapper::userToUserOutgoingDto)
                .doOnSuccess(updatedUser -> kafkaTemplate.send("users-success", "Successfully updated user with id: " + id))
                .doOnError(error -> kafkaTemplate.send("users-error","Error updating user with id: " + id));
    }

    @Override
    public Mono<Void> deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);
        return userRepository.deleteById(id)
                .doOnSuccess(v -> kafkaTemplate.send("users-success","Successfully deleted user with id: " + id))
                .doOnError(error -> kafkaTemplate.send("users-error","Error deleting user with id: " + id));
    }
}
