package com.authenticket.authenticket.controller;

import com.authenticket.authenticket.service.AmazonS3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/aws")
public class AmazonController {

    @Autowired
    private AmazonS3Service service;


    @PostMapping("/uploadFile")
    public ResponseEntity<String>  fileUpload(@RequestParam(value = "file") MultipartFile file,
                                              @RequestParam(value = "user") String user,
                                              @RequestParam(value = "file-type") String fileType){
        return new ResponseEntity<> (service.uploadFile(file, user, fileType), HttpStatus.OK);
    }
    @DeleteMapping("/deleteFile")
    public ResponseEntity<String> fileDelete(@RequestParam(value = "name") String imageName,
                                             @RequestParam(value = "user") String user,
                                             @RequestParam(value = "file-type") String fileType) {
        return new ResponseEntity<> (service.deleteFile(imageName, user, fileType), HttpStatus.OK);
    }

    @GetMapping("/displayFile")
    public ResponseEntity<String>  fileDisplay(@RequestParam(value = "name") String imageName,
                                              @RequestParam(value = "user") String user,
                                              @RequestParam(value = "file-type") String fileType){
        return new ResponseEntity<> (service.displayFile(imageName, user, fileType), HttpStatus.OK);
    }

}
