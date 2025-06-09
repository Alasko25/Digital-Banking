# Chapter 6: Security Configuration (Backend)

Welcome back, future digital banker! In our journey so far, we've built the core pieces of our backend: we learned how to structure our data with [Entities](01_entities__data_models__.md), package it neatly with [DTOs](02_data_transfer_objects__dtos__.md), talk to the database using [Repositories](03_data_access_layer__repositories__.md), and implement the banking logic in [Services](04_business_logic_layer__services__.md). Finally, in the [previous chapter](05_api_layer__rest_controllers__.md), we created the [API Layer (REST Controllers)](05_api_layer__rest_controllers__.md) to expose our banking functions to the outside world.

But imagine this: we've just opened the doors to our banking system's API. Anyone could potentially try to access customer data, transfer money, or create accounts! That's a big problem!

This is where **Security Configuration** comes in. It's like setting up a strict **bouncer** at the entrance of our API, deciding who gets in, and what they are allowed to do once they are inside.

## What is Backend Security Configuration?

The main goal of backend security is to protect our application's resources (like customer data, account balances) and ensure that only authorized users can perform specific actions.

This involves two main concepts:

1.  **Authentication:** This is about **verifying who you are**. It's like showing your ID at the entrance. "Are you really Alice Smith?" In our case, this typically happens when a user logs in by providing a username and password.
2.  **Authorization:** This is about **determining what you are allowed to do** after you've been authenticated. It's like the bouncer checking if your ticket allows you into the VIP area or just the general floor. "Okay, you are Alice, but are you allowed to view *other* customers' details, or just your own?"

In a Spring Boot application, **Spring Security** is the powerful framework we use to handle both authentication and authorization. And for modern APIs, we often use **JWT (JSON Web Tokens)** as a secure way to manage a user's authenticated state and permissions after they log in.

## The Security Flow with JWT

Here's a simplified picture of how security works with JWT:

1.  **User Logs In:** The user sends their username and password to a specific **public** API endpoint (e.g., `/auth/login`).
2.  **Backend Authenticates:** The backend verifies the username and password (checking against stored user details).
3.  **Backend Issues JWT:** If authentication is successful, the backend generates a unique **JWT token**. This token is like a secure, temporary **access ticket** containing information about the user (like their username and permissions/roles, e.g., 'USER', 'ADMIN'). The backend signs this token to ensure it hasn't been tampered with.
4.  **Client Stores JWT:** The backend sends this JWT back to the client (e.g., the web browser). The client stores it (usually in memory or local storage).
5.  **Client Makes Subsequent Requests:** For any request to a **protected** API endpoint (e.g., `/customers`, `/accounts/123/history`), the client includes the JWT in the request header (like showing your ticket every time you try to enter a club area).
6.  **Backend Validates JWT:** Spring Security intercepts the incoming request. It extracts the JWT from the header, verifies its signature (to ensure it's valid and hasn't been changed), and extracts the user's identity and permissions from the token.
7.  **Backend Authorizes Request:** Based on the permissions found in the JWT and the security rules configured for the requested endpoint, Spring Security decides if the user is authorized to access that specific resource or perform that action.
8.  **Request Proceeds or is Rejected:** If authorized, the request is allowed to proceed to our [API Controller](05_api_layer__rest_controllers__.md) and [Service Layer](04_business_logic_layer__services__.md). If not authorized, Spring Security rejects the request and sends an error response (like "401 Unauthorized" or "403 Forbidden") back to the client.

This ensures that only authenticated users with the correct permissions can access sensitive banking operations.

## Configuring Security: `SecurityConfig.java`

The main place where we define these security rules and components in Spring Security is typically a class annotated with `@Configuration` and `@EnableWebSecurity`. In our project, this is `SecurityConfig.java`.

Let's look at the key parts of `SecurityConfig.java` (simplified):

```java
// backend/src/main/java/com/example/backend/security/SecurityConfig.java

package com.example.backend.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // For method-level security like @PreAuthorize
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // To configure HTTP security rules
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // Enables Spring Security
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // For CSRF disable
import org.springframework.security.config.http.SessionCreationPolicy; // For stateless sessions
import org.springframework.security.core.userdetails.User; // For in-memory user example
import org.springframework.security.core.userdetails.UserDetailsService; // To load user details
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // Password encoder
import org.springframework.security.crypto.password.PasswordEncoder; // Password encoder interface
import org.springframework.security.oauth2.jose.jws.MacAlgorithm; // For JWT algorithm
import org.springframework.security.oauth2.jwt.JwtDecoder; // To decode JWTs
import org.springframework.security.oauth2.jwt.JwtEncoder; // To encode JWTs
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder; // Nimbus implementation for decoding
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder; // Nimbus implementation for encoding
import org.springframework.security.provisioning.InMemoryUserDetailsManager; // In-memory user store
import org.springframework.security.web.SecurityFilterChain; // The core filter chain
import org.springframework.web.cors.CorsConfiguration; // For CORS
import org.springframework.web.cors.CorsConfigurationSource; // For CORS
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // For CORS

import javax.crypto.spec.SecretKeySpec; // For JWT key

@Configuration // Tells Spring this class contains configuration
@EnableWebSecurity // Activates Spring Security features
@EnableMethodSecurity() // Allows method-level security checks (like @PreAuthorize)

public class SecurityConfig {

    // Secret key for signing/verifying JWTs (loaded from application.properties)
    @Value("${jwt.secret}") // <-- Gets value from application.properties
    private String secretKey;

    // 1. Define In-Memory Users (for this simple example)
    // In a real app, users would come from the database
    @Bean // <-- This method provides a Spring Bean (a component managed by Spring)
    public InMemoryUserDetailsManager inMemoryUserDetailsManager(){
        PasswordEncoder passwordEncoder = passwordEncoder(); // Get the password encoder
        return new InMemoryUserDetailsManager( // Create users with username, encoded password, and roles/authorities
            User.withUsername("user1").password(passwordEncoder.encode("1234")).authorities("USER").build(),
            User.withUsername("admin").password(passwordEncoder.encode("1234")).authorities("USER","ADMIN").build()
        );
    }

    // 2. Define Password Encoder (important for securely storing passwords)
    @Bean // <-- Provides a BCryptPasswordEncoder Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(); // BCrypt is a strong hashing algorithm
    }

    // 3. Define the Security Filter Chain (The core of HTTP security rules)
    @Bean // <-- Provides the main SecurityFilterChain Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return  httpSecurity
            // Configure Session Management: STATELESS means no sessions are stored server-side
            // This is crucial for JWTs, as the token itself holds the state
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Disable CSRF protection: Common for stateless APIs, as JWT handles authentication
            .csrf(AbstractHttpConfigurer::disable)
            // Enable CORS configuration (allows frontend running on a different port/domain)
            .cors(Customizer.withDefaults()) // Uses the corsConfigurationSource bean below

            // Configure Authorization Rules for HTTP Requests
            .authorizeHttpRequests(ar -> ar
                // Allow anyone to access the /auth/login endpoint (for logging in)
                .requestMatchers("/auth/login/**").permitAll()
                // Require authentication for ALL other requests
                .anyRequest().authenticated()
            )
            // Configure OAuth2 Resource Server to handle JWT authentication
            .oauth2ResourceServer(oa -> oa.jwt(Customizer.withDefaults())) // Use the JwtDecoder bean below

            // Build and return the configured security filter chain
            .build();
    }

    // 4. Configure JWT Encoder (Used by the login endpoint to create tokens)
    @Bean // <-- Provides the JwtEncoder Bean
    JwtEncoder jwtEncoder() {
        // Use the secret key loaded from application.properties
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey.getBytes()));
    }

    // 5. Configure JWT Decoder (Used by the filter chain to validate and read tokens)
    @Bean // <-- Provides the JwtDecoder Bean
    JwtDecoder jwtDecoder() {
        // Create a secret key specification from the secret key
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "RSA");
        // Build the decoder with the key and algorithm (HS512)
        return NimbusJwtDecoder.withSecretKey(secretKeySpec).macAlgorithm(MacAlgorithm.HS512).build();
    }

    // 6. Configure Authentication Manager (Used by the login endpoint to perform authentication)
    @Bean // <-- Provides the AuthenticationManager Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService){
        // DaoAuthenticationProvider uses a UserDetailsService and PasswordEncoder
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder()); // Set the password encoder
        daoAuthenticationProvider.setUserDetailsService(userDetailsService); // Set the user details service (our InMemoryUserDetailsManager bean)
        // ProviderManager is a common implementation of AuthenticationManager
        return new ProviderManager(daoAuthenticationProvider);
    }

    // 7. Configure CORS (Cross-Origin Resource Sharing)
    // Allows requests from origins other than the backend's (e.g., frontend on port 4200)
    @Bean // <-- Provides the CorsConfigurationSource Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*"); // Allow requests from ANY origin (be more specific in production!)
        corsConfiguration.addAllowedMethod("*"); // Allow ANY HTTP method (GET, POST, etc.)
        corsConfiguration.addAllowedHeader("*"); // Allow ANY header
        // corsConfiguration.setExposedHeaders(List.of("x-auth-token")); // Example of exposing specific headers
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration); // Apply this configuration to ALL paths ("/**")
        return source;
    }
}
```

Let's simplify the explanation of the key beans defined by `@Bean` methods:

*   **`inMemoryUserDetailsManager`**: This is a simple way to create users directly in memory for testing. We define "user1" (role USER) and "admin" (roles USER, ADMIN) with their passwords (which are encoded using the `passwordEncoder`). In a real application, this would load user details from a database, perhaps using a custom `UserDetailsService`.
*   **`passwordEncoder`**: This bean provides a `BCryptPasswordEncoder`. Spring Security uses this to hash passwords before storing them and to compare submitted passwords during login without storing plain text passwords.
*   **`securityFilterChain`**: This is the heart of the HTTP security configuration.
    *   `sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))`: Tells Spring *not* to create HTTP sessions. This is standard practice when using JWTs because the token itself carries the authentication information, making the server stateless.
    *   `csrf(AbstractHttpConfigurer::disable)`: Disables CSRF protection. This is also common for stateless APIs where CSRF protection is handled differently or deemed unnecessary.
    *   `cors(Customizer.withDefaults())`: Enables CORS using the configuration from the `corsConfigurationSource` bean. Essential for letting your frontend (likely on a different port or domain) talk to your backend.
    *   `authorizeHttpRequests(...)`: This is where we define URL-based access rules. `requestMatchers("/auth/login/**").permitAll()` explicitly makes the login endpoint accessible *without* authentication. `anyRequest().authenticated()` means *all other* requests require a valid authenticated user.
    *   `oauth2ResourceServer(oa -> oa.jwt(Customizer.withDefaults()))`: Configures Spring Security to act as a resource server that accepts and validates JWTs presented by clients. It uses the `JwtDecoder` bean we define.
*   **`jwtEncoder`**: This bean is used to *create* JWTs. It takes the secret key defined in `application.properties` (`jwt.secret`) to sign the tokens.
*   **`jwtDecoder`**: This bean is used to *validate* and *read* JWTs that come in with requests. It uses the *same* secret key to verify the token's signature and ensure it's valid.
*   **`authenticationManager`**: This bean is responsible for performing the actual authentication process (checking username/password). Our configuration uses a `DaoAuthenticationProvider` which relies on a `UserDetailsService` (our in-memory one) and a `PasswordEncoder`.
*   **`corsConfigurationSource`**: This bean defines the CORS rules, allowing requests from potentially different origins. For development, allowing `*` (any origin) is convenient, but in production, you'd restrict this to your frontend's domain.

## The Authentication Endpoint: `SecurityController.java`

We need a specific API endpoint where users can submit their credentials to get a JWT. This is handled by `SecurityController.java`.

```java
// backend/src/main/java/com/example/backend/security/SecurityController.java

package com.example.backend.security;

import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager; // To perform authentication
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Authentication request object
import org.springframework.security.core.Authentication; // Result of authentication
import org.springframework.security.core.GrantedAuthority; // User roles/permissions
import org.springframework.security.oauth2.jose.jws.MacAlgorithm; // For JWT algorithm
import org.springframework.security.oauth2.jwt.JwsHeader; // JWT header
import org.springframework.security.oauth2.jwt.JwtClaimsSet; // JWT payload (claims)
import org.springframework.security.oauth2.jwt.JwtEncoder; // To encode JWT
import org.springframework.security.oauth2.jwt.JwtEncoderParameters; // Parameters for encoding
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping; // For login request
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController; // API Controller

import java.time.Instant; // For timestamp
import java.time.temporal.ChronoUnit; // For token expiration
import java.util.Map; // To return response as map
import java.util.stream.Collectors; // To process authorities

@RestController // Handles web requests
@RequestMapping("/auth") // Base path for endpoints in this controller
@AllArgsConstructor // Lombok for constructor injection

public class SecurityController {
    // Inject the AuthenticationManager and JwtEncoder beans we defined in SecurityConfig
    private AuthenticationManager authenticationManager;
    private JwtEncoder jwtEncoder;

    // Endpoint for user login (handles POST requests to /auth/login)
    @PostMapping("/login")
    // Method takes username and password as request parameters
    public Map<String, String> login(String username, String password){

        // Step 1: Attempt to authenticate the user using the AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
            // Create an authentication request object with username and password
            new UsernamePasswordAuthenticationToken(username, password)
        );

        // Step 2: If authentication is successful, prepare data for JWT
        Instant instant = Instant.now(); // Current time (for token issue time)
        // Get user's authorities (roles) and format them into a space-separated string
        String scope = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(" ")); // e.g., "USER ADMIN"

        // Step 3: Build the JWT claims set (the token's payload)
        JwtClaimsSet jwtClaimsSet = JwtClaimsSet.builder()
            .issuedAt(instant) // Token issued at current time
            .expiresAt(instant.plus(10, ChronoUnit.MINUTES)) // Token expires in 10 minutes
            .subject(username) // The subject of the token (username)
            .claim("scope", scope) // Include the user's roles/authorities as a custom claim
            .build();

        // Step 4: Prepare parameters for JWT encoding
        JwtEncoderParameters jwtEncoderParameters = JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS512).build(), // Header with signing algorithm
            jwtClaimsSet // The claims we built
        );

        // Step 5: Encode the JWT using the injected JwtEncoder
        String jwt = jwtEncoder.encode(jwtEncoderParameters).getTokenValue(); // Generate the token string

        // Step 6: Return the JWT in the response body (as a simple map)
        return Map.of("access-token", jwt); // Frontend receives {"access-token": "the.jwt.string"}
    }

    // An example protected endpoint to get the authenticated user's profile
    // Requires authentication because it's not /auth/login and not permitAll
    @GetMapping("/profile")
    public Authentication authentication(Authentication authentication){
        // Spring Security automatically injects the Authentication object
        // after validating the JWT. This object contains the authenticated user details.
        return authentication; // Returns user details (username, authorities)
    }
}
```

Key points about `SecurityController`:

*   `@RestController` and `@RequestMapping("/auth")`: Defines this as an API controller handling requests starting with `/auth`.
*   `@AllArgsConstructor`: Injects the necessary `AuthenticationManager` and `JwtEncoder` beans from `SecurityConfig`.
*   `@PostMapping("/login")`: Maps the login request to the `login` method.
*   `authenticationManager.authenticate(...)`: This is where the actual username/password checking happens. It delegates to the `AuthenticationManager` we configured, which in turn uses the `UserDetailsService` and `PasswordEncoder`.
*   `JwtClaimsSet.builder()...build()`: This builds the payload of the JWT. We include the issue time, expiration time (here, 10 minutes for the example), the subject (username), and crucially, the user's roles/authorities extracted from the successful `authentication` object.
*   `jwtEncoder.encode(...)`: Uses the configured `JwtEncoder` to create the signed token string from the header and claims.
*   `return Map.of("access-token", jwt)`: Sends the generated token back to the client.

The `/auth/profile` endpoint is a simple example of a protected resource. Since it's not `/auth/login` and our filter chain says `anyRequest().authenticated()`, you *must* send a valid JWT with a request to this endpoint. Spring Security will validate the JWT and automatically provide the `Authentication` object to the method if the token is valid.

## Authorization on Protected Endpoints (`@PreAuthorize`)

While `securityFilterChain` defines *which* paths require *any* authentication, we often need more granular control based on user roles (like USER vs ADMIN). This is where `@EnableMethodSecurity` and the `@PreAuthorize` annotation come in, as seen in our [API Layer (REST Controllers)](05_api_layer__rest_controllers__.md).

Let's revisit a snippet from `CustomerRestController.java`:

```java
// Inside CustomerRestController.java (revisited with security)

@RestController
@AllArgsConstructor
@CrossOrigin("*")
public class CustomerRestController {
    private BankAccountService bankAccountService;

    @GetMapping("/customers")
    // THIS IS THE AUTHORIZATION CHECK!
    // Requires the authenticated user to have the 'SCOPE_USER' authority
    @PreAuthorize("hasAuthority('SCOPE_USER')")
    public List<CustomerDTO> customers(){
        log.info("Received request to list all customers");
        List<CustomerDTO> customerList = bankAccountService.listCostumers();
        log.info("Returning {} customers", customerList.size());
        return customerList;
    }

    @PostMapping("/customers")
    // Requires the authenticated user to have the 'SCOPE_ADMIN' authority
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public CustomerDTO saveCustomer(@RequestBody CustomerDTO customerDTO){
        log.info("Received request to save customer: {}", customerDTO.getName());
        return bankAccountService.saveCostumer(customerDTO);
    }

    // ... other methods ...
}
```

*   `@EnableMethodSecurity()` in `SecurityConfig` enables the use of annotations like `@PreAuthorize`.
*   `@PreAuthorize("hasAuthority('SCOPE_USER')")`: This annotation on the `customers()` method means that even if a request passes the basic authentication check from the `securityFilterChain` (`anyRequest().authenticated()`), it will *only* be allowed to call this specific method if the authenticated user (whose authorities are extracted from the JWT) has the authority named `SCOPE_USER`. (Note: the `SCOPE_` prefix is often used with JWTs to distinguish them from other types of authorities).
*   `@PreAuthorize("hasAuthority('SCOPE_ADMIN')")`: Similarly, the `saveCustomer` method, which creates a new customer, is restricted to users with the `SCOPE_ADMIN` authority.

This allows fine-grained control over which authenticated users can call which specific backend methods.

## Under the Hood: The Protected Request Flow

Let's visualize the path of a request for a protected resource (`/customers`) after the user has already logged in and obtained a JWT.

```mermaid
sequenceDiagram
    participant Frontend
    participant API Layer<br>(@RestController)
    participant Spring Security<br>Filter Chain
    participant JWT Decoder
    participant Authorization<br>Manager
    participant Business Logic<br>(@Service)
    participant Database

    Frontend->>API Layer<br>(@RestController): GET /customers<br/>(Includes JWT in Header)
    API Layer<br>(@RestController)-->>Spring Security<br>Filter Chain: Intercepted Request
    Spring Security<br>Filter Chain->>JWT Decoder: Validate and Decode JWT
    JWT Decoder-->>Spring Security<br>Filter Chain: Authentication Object<br/>(User, Authorities: USER)
    Spring Security<br>Filter Chain->>Authorization<br>Manager: Check if user has 'SCOPE_USER' authority for this endpoint (@PreAuthorize)
    Authorization<br>Manager-->>Spring Security<br>Filter Chain: Authorization GRANTED
    Spring Security<br>Filter Chain-->>API Layer<br>(@RestController): Request allowed to proceed
    API Layer<br>(@RestController)->>Business Logic<br>(@Service): Call listCustomers()
    Business Logic<br>(@Service)->>Database: Fetch Customer Entities (via Repository)
    Database-->>Business Logic<br>(@Service): Customer Data
    Business Logic<br>(@Service)-->>API Layer<br>(@RestController): Return List<CustomerDTO>
    API Layer<br>(@RestController)-->>Frontend: Send JSON response (List of CustomerDTOs)
```

1.  The Frontend sends an HTTP GET request to `/customers`, including the previously received JWT in a header (usually `Authorization: Bearer <token>`).
2.  Spring Security's `Filter Chain` intercepts the request *before* it reaches the specific `@GetMapping("/customers")` method handler.
3.  The `Filter Chain` uses the configured `JwtDecoder` to validate the token's signature and extract the user's information, including their authorities (e.g., 'SCOPE_USER'). If the token is invalid or expired, the chain stops here and returns an error (e.g., 401).
4.  If the token is valid, the `Filter Chain` creates an `Authentication` object representing the authenticated user.
5.  Because the target method (`customers()`) has `@PreAuthorize("hasAuthority('SCOPE_USER')`), the `Filter Chain` (or a component it delegates to) uses the `Authorization Manager` to check if the authenticated user (from the JWT) has the `SCOPE_USER` authority.
6.  If the user has the required authority, the request is allowed to proceed to the target method in the `CustomerRestController`.
7.  The controller method calls the `BankAccountService`'s `listCostumers()` method.
8.  The `Business Logic` fetches the data using the [Repositories](03_data_access_layer__repositories__.md) and database.
9.  The result ([List<CustomerDTO>](02_data_transfer_objects__dtos__.md)) is returned back through the layers.
10. The API controller formats the result into JSON and sends the HTTP response back to the Frontend.

If the user *did not* have the `SCOPE_USER` authority, step 6 would result in an authorization denial, and Spring Security would return an error (e.g., 403 Forbidden) without ever calling the controller method or the service.

## Why is Security Configuration Important?

*   **Protection:** Prevents unauthorized access to sensitive data and operations.
*   **Control:** Defines precise rules about who can do what using roles and permissions.
*   **Compliance:** Many applications, especially financial ones, have strict security requirements.
*   **Integrity:** Protects against malicious actions that could corrupt data or disrupt service.

By separating security concerns into `SecurityConfig` and using annotations like `@PreAuthorize`, our core business logic in the [Service layer](04_business_logic_layer__services__.md) and the data handling in the [Repositories](03_data_access_layer__repositories__.md) can remain focused on their primary tasks, without being cluttered by security checks.

## Summary Table (Updated)

Let's update our summary table one last time to include Security:

| Layer/Concept                    | Main Role                                                    | Works With                                   | Used By                                       | Key Spring Element(s)                                   |
| :------------------------------- | :----------------------------------------------------------- | :------------------------------------------- | :-------------------------------------------- | :------------------------------------------------------ |
| [Entities](01_entities__data_models__.md) | Blueprints for database tables & data structure              | Database                                     | Repositories, Services (internally)           | `@Entity`, `@Id`, `@OneToMany`, `@ManyToOne`            |
| [DTOs](02_data_transfer_objects__dtos__.md) | Simple data packages for transfer                            | Other application layers (API, Frontend)     | Services (for input/output), API Layer        | Plain Java Classes, Lombok (`@Data`)                    |
| [Repositories](03_data_access_layer__repositories__.md) | Access & manage data in the database                         | Database, Entities                           | Services                                      | `JpaRepository`, `@Query`                               |
| [Services (Business Logic)](04_business_logic_layer__services__.md) | Implement core business rules & orchestrate                  | Repositories, DTOs, Entities                 | API Layer, other Services                     | `@Service`, `@Transactional`                            |
| [API Layer (Controllers)](05_api_layer__rest_controllers__.md) | Receive requests, delegate to Service, send response         | DTOs, Services                               | Frontend / Other Clients                      | `@RestController`, `@GetMapping`, `@PostMapping`, etc.  |
| **Security Configuration**       | **Define access rules, authenticate users, authorize actions** | **AuthenticationManager, UserDetailsService, PasswordEncoder, JwtEncoder, JwtDecoder, API Layer** | **Spring Security Filter Chain (intercepts requests)** | **`@Configuration`, `@EnableWebSecurity`, `@EnableMethodSecurity`, `SecurityFilterChain`, `@Bean` methods (for components), `@PreAuthorize`** |

The Security Configuration layer wraps around the API layer, acting as the gatekeeper for all incoming requests, ensuring that only legitimate and authorized interactions reach our core banking logic.

## Conclusion

In this chapter, we tackled the crucial topic of **Backend Security Configuration**. We learned why it's essential to protect our banking application, covering the concepts of authentication (who you are) and authorization (what you can do). We explored how Spring Security and JWTs work together to implement this, with the user logging in to get a secure token that acts as their access ticket for future requests. We looked at the `SecurityConfig` class where rules and security components are defined, and the `SecurityController` where users can log in. Finally, we saw how `@PreAuthorize` annotations can be used on API endpoints to enforce role-based authorization.

Our backend is now not only functional but also protected. We have endpoints the frontend can call, and security measures to control access. The next steps involve building the frontend application that will consume these APIs. In the next chapter, we will explore the **Frontend Services** responsible for making HTTP requests to our secured backend API.

[Next Chapter: Frontend Services](07_frontend_services_.md)

---
