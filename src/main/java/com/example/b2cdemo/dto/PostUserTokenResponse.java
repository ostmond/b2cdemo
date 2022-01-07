package com.example.b2cdemo.dto;

public class PostUserTokenResponse {
    private String id_token;
    
    private String access_token;

    public String getId_token() {
        return this.id_token;
    }

    public void setId_token(String id_token) {
        this.id_token = id_token;
    }

    public String getAccess_token() {
        return this.access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }
        
}
