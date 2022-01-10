package com.example.b2cdemo.service;

import java.util.Arrays;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.PasswordProfile;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class B2CUserCreationService {
    @Value("${azure.adb2c.application.id}")
    private String clientId;

    @Value("${azure.adb2c.client.secret}")
    private String clientSecret;

    @Value("${azure.adb2c.tenant.id}")
    private String tenantId;

    @Value("${azure.adb2c.tenant.name}")
    private String tenantName;

    @Value("${microsoft.graph.scope}")
    private String scope;

    public void createUser(final String firstName, final String lastName) {
        final ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
            .clientId(clientId)
            .clientSecret(clientSecret) //required for web apps, do not set for native apps
            .tenantId(tenantId)
            .build();

        final TokenCredentialAuthProvider tokenCredentialAuthProvider = new TokenCredentialAuthProvider(Arrays.asList(scope), clientSecretCredential);

        GraphServiceClient graphClient = GraphServiceClient.builder()
                        .authenticationProvider(tokenCredentialAuthProvider)
                        .buildClient();

        User user = new User();
        user.accountEnabled = true;
        user.displayName = firstName + " " + lastName;
        user.mailNickname = firstName + lastName;
        user.userPrincipalName = user.mailNickname + "@" + tenantName + ".onmicrosoft.com";
        user.passwordPolicies = "DisablePasswordExpiration";
        PasswordProfile passwordProfile = new PasswordProfile();
        passwordProfile.forceChangePasswordNextSignIn = false;
        passwordProfile.password = "xWwvJ]6NMw+bWH-d";
        user.passwordProfile = passwordProfile;
        
        graphClient.users()
            .buildRequest()
            .post(user);
    }
}