package com.listen.gallery.service;

import com.listen.gallery.model.UserInfo;
import com.listen.gallery.repository.UserInfoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserInfoService {

    private static final Logger logger = LoggerFactory.getLogger(UserInfoService.class);

    private final UserInfoRepository repo;

    public UserInfoService(UserInfoRepository repo) {
        this.repo = repo;
    }

    public List<UserInfo> getAllUsers() {
        logger.info("Fetching all users");
        List<UserInfo> users = repo.findAll();
        logger.info("Found {} users", users.size());
        return users;
    }

    public Optional<UserInfo> getUserById(Integer id) {
        logger.info("Fetching user by id: {}", id);
        Optional<UserInfo> user = repo.findById(id);
        if (user.isPresent()) {
            logger.info("Found user: {}", user.get());
        } else {
            logger.warn("User with id: {} not found", id);
        }
        return user;
    }

    public void saveUser(UserInfo userInfo) {
        logger.info("Attempting to save user: {}", userInfo.getId());
        if (userInfo.getId() != null && repo.existsById(userInfo.getId())) {
            logger.info("User with id: {} exists. Performing an update.", userInfo.getId());
            repo.findById(userInfo.getId())
                    .map(existingUser -> {
                        existingUser.setName(userInfo.getName());
                        existingUser.setAge(userInfo.getAge());
                        UserInfo savedUser = repo.save(existingUser);
                        logger.info("Successfully updated user: {}", savedUser);
                        return savedUser;
                    })
                    .orElseGet(() -> {
                        // This case is rare, but good to log
                        logger.warn("User with id: {} was expected to exist but not found. Creating a new one instead.", userInfo.getId());
                        UserInfo savedUser = repo.save(userInfo);
                        logger.info("Successfully created user (fallback): {}", savedUser);
                        return savedUser;
                    });
        } else {
            logger.info("User does not exist or has no ID. Performing a create.");
            UserInfo savedUser = repo.save(userInfo);
            logger.info("Successfully created user: {}", savedUser);
        }
    }

    public void deleteUser(Integer id) {
        logger.info("Attempting to delete user with id: {}", id);
        repo.deleteById(id);
        logger.info("Successfully deleted user with id: {}", id);
    }
}