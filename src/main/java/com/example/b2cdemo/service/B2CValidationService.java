package com.example.b2cdemo.service;

import com.example.b2cdemo.dto.azure.KeyBean;
import com.example.b2cdemo.dto.azure.OpenIdConfigurationBean;
import com.example.b2cdemo.dto.azure.OpenIdKeysBean;
import com.example.b2cdemo.exception.TokenException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.KeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * This class is leart from https://www.lionmint.com/en/spring-boot-security-with-azure-b2c-2/
 * His complete project can be found on https://github.com/lionmint/spring-angular-azureb2c/tree/master/myapiboot
 */
@Service
public class B2CValidationService {
    private static Logger log = LoggerFactory.getLogger(B2CValidationService.class);

    // see https://docs.microsoft.com/en-us/azure/active-directory-b2c/openid-connect
    @Value("${azure.adb2c.openid.configuration.url}")
    private String azureOpenIdConfigUrl;
    
    @Value("${azure.adb2c.application.id}")
    private String applicationId;

    public String getUsernameFromToken(String idToken) throws TokenException {
        String username = "";
        Map<String, Object> map = getTokenComponents(idToken);
        if (map != null && map.containsKey("body")) {
            Map<String, Object> tokenBody = (Map<String, Object>) map.get("body");
            if (tokenBody != null && tokenBody.containsKey("emails")) {
                java.util.ArrayList<String> usernames = (java.util.ArrayList<String>) tokenBody.get("emails");
                if (usernames != null && !usernames.isEmpty()) {
                    username = usernames.get(0);
                }
            }
        }
        return username;
    }

    public boolean isNewUsernameFromToken(String idToken) throws TokenException {
        boolean isNewUser = false;
        Map<String, Object> map = getTokenComponents(idToken);
        if (map != null && map.containsKey("body")) {
            Map<String, Object> tokenBody = (Map<String, Object>) map.get("body");
            if (tokenBody != null && tokenBody.containsKey("newUser")) {
                Boolean isnewObj = (Boolean) tokenBody.get("newUser");
                if (isnewObj != null && isnewObj.booleanValue()) {
                    isNewUser = true;
                }
            }
        }
        return isNewUser;
    }

    public boolean validateToken(String idToken) {
        boolean isValidToken = false;
        try {
            Map<String, Object> tokenHeader = new HashMap<String, Object>();
            String signatureJws = "";
            Map<String, Object> mapTokenComponents = getTokenComponents(idToken);
            tokenHeader = (Map<String, Object>) mapTokenComponents.get("header");
            signatureJws = (String) mapTokenComponents.get("signature");

            //(2) GET OPENID CONFIGURATIONS AND SELECT THE MACHING KEY BEAN
            String keysUrl = callOpenidConfiguration().getJwks_uri();
            log.info("key url:: {}", keysUrl);
            KeyBean keyBeanForAccess = null;
            for (KeyBean keyBean : discoveryKeys(keysUrl).getKeys()) {
                log.info("kid:: ", keyBean.getKid());
                if (keyBean.getKid().equals((String) tokenHeader.get("kid"))) {
                    keyBeanForAccess = keyBean;
                    break;
                }
            }

            //(3) VALIDATE THE JWT CLAIMS
            log.info("VALIDATE THE JWT CLAIMS");
            PublicKey pubKeyNew = null;
            Claims claims = null;
            try {
                byte[] modulusBytes = Base64.getUrlDecoder().decode(keyBeanForAccess.getN());
                byte[] exponentBytes = Base64.getUrlDecoder().decode(keyBeanForAccess.getE());
                BigInteger modulusInt = new BigInteger(1, modulusBytes);
                BigInteger exponentInt = new BigInteger(1, exponentBytes);
                KeySpec publicSpec = null;

                KeyFactory keyFactory = KeyFactory.getInstance(keyBeanForAccess.getKty());
                switch (keyBeanForAccess.getKty()) {
                    case "RSA":
                        publicSpec = new RSAPublicKeySpec(modulusInt, exponentInt);
                        break;
                }
                pubKeyNew = keyFactory.generatePublic(publicSpec);
                log.info("pubKeyNew: {}", pubKeyNew);
                claims = Jwts.parser()
                        .setSigningKey(pubKeyNew)
                        .parseClaimsJws(idToken).getBody();
                log.info("Expiration Date:: " + claims.getExpiration().toString());
                log.info("Issued Date:: " + claims.getIssuedAt().toString());
                log.info("Issuer:: " + claims.getIssuer());
                log.info("Audience:: " + claims.getAudience());
            } catch (Exception e) {
                throw new TokenException(500, "Invalid claims: " + e.getMessage());
            }

            if (claims == null || !applicationId.equals(claims.getAudience())) {
                throw new TokenException(500, "Invalid audience claim");
            }

            //(4) VERIFY SIGNATURE
            try {
                byte[] signature = Base64.getUrlDecoder().decode(signatureJws);
                Signature sig = Signature.getInstance("SHA256withRSA");
                sig.initVerify(pubKeyNew);
                sig.update(idToken.getBytes());
                log.info("The signature is {}", sig.verify(signature));
            } catch (Exception e) {
                throw new TokenException(500, "Invalid signature:: " + e.getMessage());
            }

            isValidToken = true;

        } catch (TokenException e) {
            log.warn("Invalid token!", e);
        }

        return isValidToken;
    }

    public OpenIdConfigurationBean callOpenidConfiguration(String url) {
        azureOpenIdConfigUrl = url;
        return callOpenidConfiguration();
    }

    public OpenIdConfigurationBean callOpenidConfiguration() {
        URL url;
        OpenIdConfigurationBean openIdConfigurationBean = new OpenIdConfigurationBean();
        try {

            url = new URL(azureOpenIdConfigUrl);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

            if (con != null) {
                try {

                    BufferedReader br =
                            new BufferedReader(
                                    new InputStreamReader(con.getInputStream()));

                    String input;
                    StringBuilder builder = new StringBuilder();

                    while ((input = br.readLine()) != null) {
                        builder.append(input);
                    }
                    br.close();

                    ObjectMapper mapper = new ObjectMapper();
                    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    openIdConfigurationBean = mapper.readValue(builder.toString(), OpenIdConfigurationBean.class);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return openIdConfigurationBean;
    }

    public OpenIdKeysBean discoveryKeys(String keysURL) {
        URL url;
        OpenIdKeysBean openIdKeysBean = new OpenIdKeysBean();
        try {

            url = new URL(keysURL);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

            if (con != null) {
                try {

                    BufferedReader br =
                            new BufferedReader(
                                    new InputStreamReader(con.getInputStream()));

                    String input;
                    StringBuilder builder = new StringBuilder();

                    while ((input = br.readLine()) != null) {
                        builder.append(input);
                    }
                    br.close();

                    ObjectMapper mapper = new ObjectMapper();
                    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    openIdKeysBean = mapper.readValue(builder.toString(), OpenIdKeysBean.class);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return openIdKeysBean;
    }

    public Map<String, Object> string2JSONMap(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = new HashMap<String, Object>();

        // convert JSON string to Map
        map = mapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
        return map;
    }

    public void setAzureOpenIdConfigUrl(String azureOpenIdConfigUrl) {
        this.azureOpenIdConfigUrl = azureOpenIdConfigUrl;
    }

    private Map<String, Object> getTokenComponents(String idToken) throws TokenException {
        Decoder decoder = Base64.getDecoder();
        StringTokenizer tokenizer = new StringTokenizer(idToken, ".");
        int i = 0;
        Map<String, Object> tokenHeader = new HashMap<String, Object>();
        Map<String, Object> tokenBody = new HashMap<String, Object>();
        String signatureJws = "";
        Map<String, Object> tokenMapParts = new HashMap<String, Object>();

        //(1) DECODE THE 3 PARTS OF THE JWT TOKEN
        try {
            while (tokenizer.hasMoreElements()) {
                if (i == 0) {
                    tokenHeader = string2JSONMap(new String(decoder.decode(tokenizer.nextToken())));
                } else if (i == 1) {
                    tokenBody = string2JSONMap(new String(decoder.decode(tokenizer.nextToken())));
                } else {
                    signatureJws = new String(tokenizer.nextToken());
                    log.info(signatureJws);
                }
                i++;
            }
        } catch (IOException e) {
            throw new TokenException(500, e.getMessage());
        }

        //(1.1) THE 3 PARTS OF THE TOKEN SHOULD BE IN PLACE
        if (tokenHeader == null || tokenBody == null || signatureJws == null || tokenHeader.isEmpty() || tokenBody.isEmpty() || signatureJws.isEmpty()) {
            throw new TokenException(500, "Invalid Token");
        }

        tokenMapParts.put("header", tokenHeader);
        tokenMapParts.put("body", tokenBody);
        tokenMapParts.put("signature", signatureJws);

        return tokenMapParts;
    }
}
