package com.example.b2cdemo.controller;

import com.example.b2cdemo.dto.PostUserCreationRequest;
import com.example.b2cdemo.dto.PostUserTokenRequest;
import com.example.b2cdemo.dto.PostUserTokenResponse;
import com.example.b2cdemo.service.B2CTokenService;
import com.example.b2cdemo.service.B2CUserCreationService;
import com.example.b2cdemo.service.B2CValidationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class B2CUserController {
    private final B2CUserCreationService b2cUserCreationService;
    private final B2CTokenService b2cTokenService;
    private final B2CValidationService b2cValidationService;

    @Autowired
    public B2CUserController(B2CUserCreationService b2cUserCreationService, 
                                B2CTokenService b2cTokenService,
                                B2CValidationService b2cValidationService) {
        this.b2cUserCreationService = b2cUserCreationService;
        this.b2cTokenService = b2cTokenService;
        this.b2cValidationService = b2cValidationService;
    }
    
    @PostMapping("/name")
    public ResponseEntity<Void> createRandomUser(@RequestBody PostUserCreationRequest postUserCreationRequest) {
        b2cUserCreationService.createUser(postUserCreationRequest.getFirstName(), postUserCreationRequest.getLastName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/token")
    public ResponseEntity<PostUserTokenResponse> getUserToken(@RequestBody PostUserTokenRequest postUserTokenRequest) {
        PostUserTokenResponse postUserTokenResponse = b2cTokenService.getUserAccessToken(postUserTokenRequest.getUsername(), postUserTokenRequest.getPassword());
        return ResponseEntity.status(HttpStatus.OK).body(postUserTokenResponse);
    }

    @PostMapping("/validation")
    public ResponseEntity<Boolean> validUserToken(@RequestBody PostUserTokenRequest postUserTokenRequest,
                                                    @RequestHeader(value="Authorization") String bearerString) {
        String[] authorizationParts = bearerString.split(" ");
        Boolean result = b2cValidationService.validateToken(authorizationParts[1]);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
    
}
