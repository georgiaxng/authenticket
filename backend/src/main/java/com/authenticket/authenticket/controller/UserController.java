package com.authenticket.authenticket.controller;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.authenticket.authenticket.controller.response.GeneralApiResponse;
import com.authenticket.authenticket.dto.user.UserDisplayDto;
import com.authenticket.authenticket.dto.user.UserFullDisplayDto;
import com.authenticket.authenticket.exception.NonExistentException;
import com.authenticket.authenticket.model.Event;
import com.authenticket.authenticket.model.User;
import com.authenticket.authenticket.repository.UserRepository;
import com.authenticket.authenticket.service.PresaleService;
import com.authenticket.authenticket.service.Utility;
import com.authenticket.authenticket.service.impl.AmazonS3ServiceImpl;
import com.authenticket.authenticket.service.impl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(
        origins = {
                "${authenticket.frontend-production-url}",
                "${authenticket.frontend-dev-url}",
                "${authenticket.loadbalancer-url}"
        },
        methods = {RequestMethod.GET, RequestMethod.PUT},
        allowedHeaders = {"Authorization", "Cache-Control", "Content-Type"},
        allowCredentials = "true"
)
@RequestMapping(path = "/api/user")
public class UserController extends Utility {
    private final UserRepository userRepository;

    private final UserServiceImpl userService;

    private final AmazonS3ServiceImpl amazonS3Service;

    private final PresaleService presaleService;

    @Autowired
    public UserController(UserRepository userRepository, UserServiceImpl userService, AmazonS3ServiceImpl amazonS3Service, PresaleService presaleService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.amazonS3Service = amazonS3Service;
        this.presaleService = presaleService;
    }

    @GetMapping("/test")
    public String test() {
        return "test successful";
    }

    @GetMapping
    public ResponseEntity<GeneralApiResponse<Object>> findAllUser() {
        List<UserFullDisplayDto> userList = userService.findAllUser();
        if (userList.isEmpty()) {
            return ResponseEntity.ok(generateApiResponse(userList, "No event user found."));

        } else {
            return ResponseEntity.ok(generateApiResponse(userList, "User successfully returned."));

        }
    }

    @GetMapping("/interested-events")
    public ResponseEntity<GeneralApiResponse<Object>> findEventsOfInterestToUser(@NonNull HttpServletRequest request) {
        User user = retrieveUserFromRequest(request);
        List<Event> events = presaleService.findEventsByUser(user);
        return ResponseEntity.ok(generateApiResponse(events, "User has indicated interest for " + events.size() + " events."));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<GeneralApiResponse<Object>> findUserById(@PathVariable("userId") Integer userId) {
        Optional<UserFullDisplayDto> userDisplayDto = userService.findById(userId);
        return userDisplayDto.map(
                        displayDto -> ResponseEntity.status(200).body(
                                generateApiResponse(displayDto, "User found")
                        ))
                .orElseGet(() -> ResponseEntity.status(400).body(generateApiResponse(null, "User does not exist")));
    }

    @PutMapping("/updateUserProfile")
    public ResponseEntity<GeneralApiResponse<Object>> updateUser(@RequestBody User newUser,
                                                                 @NonNull HttpServletRequest request) {
        User user = retrieveUserFromRequest(request);
        Optional<User> optionalUser = userRepository.findByEmail(newUser.getEmail());
        if (optionalUser.isPresent()) {
            if (!optionalUser.get().equals(user)) {
                throw new IllegalArgumentException("Cannot update other user's profile");
            }
            UserDisplayDto updatedUser = userService.updateUser(newUser);
            return ResponseEntity.status(200).body(generateApiResponse(updatedUser, "User has been successfully updated"));
        }

        return ResponseEntity.status(404).body(generateApiResponse(null, "User does not exist"));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<GeneralApiResponse<Object>> deleteUser(@PathVariable("userId") Integer userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(generateApiResponse(null, String.format("User %d Deleted Successfully", userId)));

    }

    @PutMapping("/updateUserImage")
    public ResponseEntity<GeneralApiResponse<Object>> updateProfileImage(@RequestParam("profileImage") MultipartFile profileImage,
                                                                         @RequestParam("imageName") String imageName,
                                                                         @NonNull HttpServletRequest request) {
        User user = retrieveUserFromRequest(request);
        try {
            amazonS3Service.uploadFile(profileImage, imageName, "user_profile");
            return ResponseEntity.ok(generateApiResponse(userService.updateProfileImage(imageName, user.getUserId()), "Profile Image Uploaded Successfully."));
        } catch (AmazonS3Exception e) {
            String errorCode = e.getErrorCode();
            if ("AccessDenied".equals(errorCode)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(generateApiResponse(null, "Access Denied to Amazon."));
            } else if ("NoSuchBucket".equals(errorCode)) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(generateApiResponse(null, "S3 bucket not found."));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(generateApiResponse(null, "An error occurred during S3 interaction."));
            }
        }
    }
}
