This project demos how you can via Spring-boot/Java 11 programatically create new user into Azure AD B2C, get the user id token as well as the access token and validate the id token

# How to run the project:
1. Set the missing properties in application.properties
2. Run "mvn clean spring-boot:run"

# The explanation:
1. [How to Create User in Azure AD B2C by using Microsoft Graph and Java](https://siweheee.medium.com/how-to-create-user-in-azure-ad-b2c-by-using-microsoft-graph-and-java-4ac3e18b298e)
2. [How to Get and Validate User Tokens Issued by Azure AD B2C via Java](https://siweheee.medium.com/how-to-get-user-access-token-from-azure-ad-b2c-via-java-d70503d65309)
   
# Acknowledgement:
The part of token validation is learnt from  https://github.com/lionmint/spring-angular-azureb2c/tree/master/myapiboot  