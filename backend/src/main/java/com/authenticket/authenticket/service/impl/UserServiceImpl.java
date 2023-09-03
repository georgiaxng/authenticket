package com.authenticket.authenticket.service.impl;

import com.authenticket.authenticket.dto.user.UserDisplayDto;
import com.authenticket.authenticket.dto.user.UserDtoMapper;
import com.authenticket.authenticket.model.Admin;
import com.authenticket.authenticket.model.User;
import com.authenticket.authenticket.repository.UserRepository;
import com.authenticket.authenticket.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private final static String USER_NOT_FOUND_MSG = "user with email %s not found";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDtoMapper userDtoMapper;

    @Autowired
    private AmazonS3ServiceImpl amazonS3Service;

    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException{
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        String.format(USER_NOT_FOUND_MSG, email)));
    }

    public Optional<UserDisplayDto> findById(Integer userId) {
        return Optional.empty();
    }

    public UserDisplayDto updateUser(User newUser){
        Optional<User> userOptional = userRepository.findByEmail(newUser.getEmail());

        if(userOptional.isPresent()){
            User existingUser = userOptional.get();
            userDtoMapper.update(newUser, existingUser);
            userRepository.save(existingUser);
            return userDtoMapper.apply(existingUser);
        }

        return null;
    }

    public String removeUser(Integer userId){
        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if(user.getDeletedAt()!=null){
                return "user already deleted";
            }

            user.setDeletedAt(LocalDateTime.now());
            userRepository.save(user);
            return "user deleted successfully";
        }

        return "error: user deleted unsuccessfully, user might not exist";
    }

    public UserDisplayDto updateProfileImage(String filename, String userEmail){
        Optional<User> userOptional = userRepository.findByEmail(userEmail);

        if(userOptional.isPresent()){
            User existingUser = userOptional.get();
            existingUser.setProfileImage(filename);
            userRepository.save(existingUser);
            return userDtoMapper.apply(existingUser);
        }
        return null;
    }
}