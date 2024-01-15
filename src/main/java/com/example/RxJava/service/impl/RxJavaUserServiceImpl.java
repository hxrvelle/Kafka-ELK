package com.example.RxJava.service.impl;

import com.example.RxJava.controller.dto.UserIncomingDto;
import com.example.RxJava.controller.dto.UserOutgoingDto;
import com.example.RxJava.controller.mapper.UserMapper;
import com.example.RxJava.model.User;
import com.example.RxJava.repository.RxJavaUserRepository;
import com.example.RxJava.service.RxJavaUserService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RxJavaUserServiceImpl implements RxJavaUserService {
    private final RxJavaUserRepository rxJavaUserRepository;
    private final UserMapper userMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public RxJavaUserServiceImpl(RxJavaUserRepository rxJavaUserRepository, UserMapper userMapper, KafkaTemplate<String, String> kafkaTemplate) {
        this.rxJavaUserRepository = rxJavaUserRepository;
        this.userMapper = userMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Flowable<UserOutgoingDto> getAllUsers() {
        log.info("Getting all users");
        return rxJavaUserRepository.findAll()
                .map(userMapper::userToUserOutgoingDto)
                .doOnComplete(() -> kafkaTemplate.send("users-success", "Successfully retrieved all users"))
                .doOnError(error -> kafkaTemplate.send("users-error","Error retrieving all users"));
    }

    @Override
    public Maybe<UserOutgoingDto> getUserById(Long id) {
        log.info("Getting user by id: {}", id);
        return rxJavaUserRepository.findById(id)
                .map(userMapper::userToUserOutgoingDto)
                .doOnSuccess(user -> kafkaTemplate.send("users-success", "Successfully retrieved user with id: " + id))
                .doOnError(error -> kafkaTemplate.send("users-error","Error retrieving user with id: " + id));
    }

    @Override
    public Single<UserOutgoingDto> addUser(UserIncomingDto userIncomingDto) {
        log.info("Adding user: {}", userIncomingDto);
        User user = userMapper.userIncomingDtoToUser(userIncomingDto);
        return rxJavaUserRepository.save(user)
                .map(userMapper::userToUserOutgoingDto)
                .doOnSuccess(savedUser -> kafkaTemplate.send("users-success", "Successfully added user: " + savedUser))
                .doOnError(error -> kafkaTemplate.send("users-error","Error adding user: " + userIncomingDto));
    }

    @Override
    public Maybe<UserOutgoingDto> updateUser(Long id, UserIncomingDto userIncomingDto) {
        log.info("Updating user with id: {}", id);
        User user = userMapper.userIncomingDtoToUser(userIncomingDto);
        return rxJavaUserRepository.findById(id)
                .flatMap(existingUser -> {
                    existingUser.setName(user.getName());
                    existingUser.setAge(user.getAge());
                    return rxJavaUserRepository.save(existingUser).toMaybe();
                })
                .map(userMapper::userToUserOutgoingDto)
                .doOnSuccess(updatedUser -> kafkaTemplate.send("users-success", "Successfully updated user with id: " + id))
                .doOnError(error -> kafkaTemplate.send("users-error","Error updating user with id: " + id));
    }

    @Override
    public Completable deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);
        return rxJavaUserRepository.deleteById(id)
                .doOnComplete(() -> kafkaTemplate.send("users-success","Successfully deleted user with id: " + id))
                .doOnError(error -> kafkaTemplate.send("users-error","Error deleting user with id: " + id));
    }
}