package com.example.b2cdemo.service;

import com.example.b2cdemo.dto.PostUserTokenResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class B2CTokenService {
    @Value("${azure.adb2c.tenant.token.validation.url}")
    private String url;

    @Value("${azure.adb2c.application.id}")
    private String clientId;

    @Value("${azure.adb2c.tenant.token.grant.type}")
    private String grantType;

    @Value("${azure.adb2c.tenant.token.scope}")
    private String scope;
    
    private RestTemplate restTemplate = new RestTemplate();
    
    public PostUserTokenResponse getUserAccessToken(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
        map.add("client_id", clientId);
        map.add("grant_type", grantType);
        map.add("scope", scope);
        map.add("username", username);
        map.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        ResponseEntity<PostUserTokenResponse> response = restTemplate.postForEntity(url, request , PostUserTokenResponse.class);

        return response.getBody();
    }
    
}
