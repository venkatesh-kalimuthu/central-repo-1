package com.ford.decisionplatform.kbsservice.config;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {


    @Value("${cn.app.security.azure-ad.jwtdecoder.client-id}")
    private String clientId;

    @Value("${cn.app.security.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${google-issuer-uri}")
    private String GOOGLE_ISSUER_URI ;



    @Bean
    @Order(0)
    SecurityFilterChain defaultAzureAdFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(request -> {
                    String authHeader = request.getHeader("Authorization");
                    return authHeader != null && authHeader.contains("Bearer") && isAzureToken(authHeader);
                })
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                     .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/scoring/request/**").hasAuthority("write")
                        .requestMatchers("/eca/request/**").hasAuthority("write")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(customJwtAuthenticationConverter())   ) // ← non-deprecated form)
                );

        return http.build();
    }

    private JwtAuthenticationConverter customJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) {
                return List.of();
            }
            return roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });

        return converter;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html");
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Build decoder with custom RestOperations
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withIssuerLocation(issuerUri)
                //.restOperations(restTemplate)
                .build();

        OAuth2TokenValidator<Jwt> withIssuer =
                JwtValidators.createDefaultWithIssuer(issuerUri);

        OAuth2TokenValidator<Jwt> audienceValidator =
                new AudienceValidator(clientId);

        DelegatingOAuth2TokenValidator<Jwt> validator =
                new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain cloudRunInternalPermitIfValidTokenFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(request -> {
                    String authHeader = request.getHeader("Authorization");
                    return authHeader != null && authHeader.contains("Bearer") && isGoogleToken(authHeader);
                })
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(googleJwtDecoder()))
                );
        return http.build();
    }

    // JWT Decoder for Google-issued ID Tokens
    @Bean
    public JwtDecoder googleJwtDecoder() {
        return NimbusJwtDecoder.withIssuerLocation(GOOGLE_ISSUER_URI)

                .build();
    }




    private String extractIssuer(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode jsonNode = new ObjectMapper().readTree(payload);
            return jsonNode.has("iss") ? jsonNode.get("iss").asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isAzureToken(String authHeader) {
        String token = extractBearerToken(authHeader);
        String issuer = extractIssuer(token);
        return issuer != null && issuer.equals(issuerUri); // Azure issuer
    }

    private boolean isGoogleToken(String authHeader) {
        String token = extractBearerToken(authHeader);
        String issuer = extractIssuer(token);
        return issuer != null && issuer.equals(GOOGLE_ISSUER_URI); // Google issuer
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }


}
 
