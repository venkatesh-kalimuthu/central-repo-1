package com.ford.decisionplatform.kbsservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.time.Clock;
import java.time.Duration;

@Slf4j
@Configuration
public class WebClientConfig {

    // Scoring API specific client details
    @Value("${spring.security.oauth2.client.provider.scoring-api.token-uri}")
    private String scoringApiTokenURI;
    @Value("${spring.security.oauth2.client.registration.scoring-api.authorization-grant-type}")
    private String scoringAuthGrantType;
    @Value("${spring.security.oauth2.client.registration.scoring-api.client-id}")
    private String scoringApiClientID;
    @Value("${spring.security.oauth2.client.registration.scoring-api.client-secret}")
    private String scoringApiClientSecret;

    // ECA API specific client details
    @Value("${spring.security.oauth2.client.provider.eca-api.token-uri}")
    private String ecaApiTokenURI;
    @Value("${spring.security.oauth2.client.registration.eca-api.authorization-grant-type}")
    private String ecaAuthGrantType;
    @Value("${spring.security.oauth2.client.registration.eca-api.client-id}")
    private String ecaApiClientID;
    @Value("${spring.security.oauth2.client.registration.eca-api.client-secret}")
    private String ecaApiClientSecret;

    // Scopes for Scoring API
    @Value("${spring.security.oauth2.client.registration.scoring-api.c-scope}")
    private String scoringApiCommercialScope;
    @Value("${spring.security.oauth2.client.registration.scoring-api.ci-scope}")
    private String scoringApiCommercialIndividualScope;

    // Scopes for ECA API
    @Value("${spring.security.oauth2.client.registration.eca-api.c-scope}")
    private String ecaApiCommercialScope;
    @Value("${spring.security.oauth2.client.registration.eca-api.ci-scope}")
    private String ecaApiCommercialIndividualScope;

    // Proxy Details
    @Value("${proxy.http-host}")
    private String proxyHost;
    @Value("${proxy.http-port}")
    private int proxyPort;


    private static final String SCORING_API_C = "scoring-api-commercial";
    private static final String ECA_API_C = "eca-api-commercial";
    private static final String SCORING_API_CI = "scoring-api-commercial-individual";
    private static final String ECA_API_CI = "eca-api-commercial-individual";

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        var scoringApiC = ClientRegistration.withRegistrationId(SCORING_API_C)
                .scope(scoringApiCommercialScope)
                .tokenUri(scoringApiTokenURI)
                .clientId(scoringApiClientID)
                .clientSecret(scoringApiClientSecret)
                .authorizationGrantType(new AuthorizationGrantType(scoringAuthGrantType))
                .build();

        var ecaApiC = ClientRegistration.withRegistrationId(ECA_API_C)
                .scope(ecaApiCommercialScope)
                .tokenUri(ecaApiTokenURI)
                .clientId(ecaApiClientID)
                .clientSecret(ecaApiClientSecret)
                .authorizationGrantType(new AuthorizationGrantType(ecaAuthGrantType))
                .build();

        var scoringApiCI = ClientRegistration.withRegistrationId(SCORING_API_CI)
                .scope(scoringApiCommercialIndividualScope)
                .tokenUri(scoringApiTokenURI)
                .clientId(scoringApiClientID)
                .clientSecret(scoringApiClientSecret)
                .authorizationGrantType(new AuthorizationGrantType(scoringAuthGrantType))
                .build();

        var ecaApiCI = ClientRegistration.withRegistrationId(ECA_API_CI)
                .scope(ecaApiCommercialIndividualScope)
                .tokenUri(ecaApiTokenURI)
                .clientId(ecaApiClientID)
                .clientSecret(ecaApiClientSecret)
                .authorizationGrantType(new AuthorizationGrantType(ecaAuthGrantType))
                .build();

        return new InMemoryClientRegistrationRepository(scoringApiC, ecaApiC, scoringApiCI, ecaApiCI);
    }

    @Bean("scoring-api-commercial")
    public RestClient scoringAPIServiceC(OAuth2AuthorizedClientManager authorizedClientManager) {
        var oauthInterceptor = new CustomOAuth2ClientHttpRequestInterceptor(authorizedClientManager, SCORING_API_C);
        return RestClient.builder()
                .requestInterceptor(oauthInterceptor)
                .build();
    }

    @Bean("eca-api-commercial")
    RestClient ecaAPIServiceC(OAuth2AuthorizedClientManager authorizedClientManager) {
        var oauthInterceptor = new CustomOAuth2ClientHttpRequestInterceptor(authorizedClientManager, ECA_API_C);
        return RestClient.builder()
                .requestInterceptor(oauthInterceptor)
                .build();
    }

    @Bean("scoring-api-commercial-individual")
    RestClient scoringAPIServiceCI(OAuth2AuthorizedClientManager authorizedClientManager) {
        var oauthInterceptor = new CustomOAuth2ClientHttpRequestInterceptor(authorizedClientManager, SCORING_API_CI);
        return RestClient.builder()
                .requestInterceptor(oauthInterceptor)
                .build();
    }

    @Bean("eca-api-commercial-individual")
    RestClient ecaAPIServiceCI(OAuth2AuthorizedClientManager authorizedClientManager) {
        var oauthInterceptor = new CustomOAuth2ClientHttpRequestInterceptor(authorizedClientManager, ECA_API_CI);
        return RestClient.builder()
                .requestInterceptor(oauthInterceptor)
                .build();
    }


    // Create a single shared OAuth2AuthorizedClientService bean
    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    // Create a single shared OAuth2AuthorizedClientManager bean with token refresh capability
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setProxy(proxy);

        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setMessageConverters(List.of(
                new FormHttpMessageConverter(),
                new OAuth2AccessTokenResponseHttpMessageConverter()
        ));
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

        var tokenResponseClient = new DefaultClientCredentialsTokenResponseClient();
        tokenResponseClient.setRestOperations(restTemplate);

        var credentialsProvider = new ClientCredentialsOAuth2AuthorizedClientProvider();
        credentialsProvider.setAccessTokenResponseClient(tokenResponseClient);
        // Set clock skew to refresh token 60 seconds before expiration
        credentialsProvider.setClock(Clock.systemUTC());
        credentialsProvider.setClockSkew(Duration.ofSeconds(60));

        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                authorizedClientService
        );
        manager.setAuthorizedClientProvider(credentialsProvider);

        return manager;
    }


    private static class CustomOAuth2ClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
        private final OAuth2AuthorizedClientManager authorizedClientManager;
        private final String clientRegistrationId;

        public CustomOAuth2ClientHttpRequestInterceptor(OAuth2AuthorizedClientManager manager, String id) {
            this.authorizedClientManager = manager;
            this.clientRegistrationId = id;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            var principal = new OAuth2AuthenticationTokenPrincipal(clientRegistrationId);
            var context = OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistrationId)
                    .principal(principal)
                    .build();

            // The manager will automatically reuse cached tokens and refresh when needed
            var client = authorizedClientManager.authorize(context);
            if (client != null && client.getAccessToken() != null) {
                log.debug("Using OAuth2 token for client: {}, expires at: {}",
                        clientRegistrationId,
                        client.getAccessToken().getExpiresAt());
                request.getHeaders().add("Authorization", "Bearer " + client.getAccessToken().getTokenValue());
            } else {
                log.warn("No valid OAuth2 token available for client: {}", clientRegistrationId);
            }

            return execution.execute(request, body);
        }
    }

    private static class OAuth2AuthenticationTokenPrincipal implements org.springframework.security.core.Authentication {
        private final String name;
        public OAuth2AuthenticationTokenPrincipal(String name) { this.name = name; }
        @Override public String getName() { return name; }
        @Override
        public Object getCredentials() {
            // Not required for client credentials grant token retrieval
            return null;
        }

        @Override
        public Object getDetails() {
            // Not required for client credentials grant token retrieval
            return null;
        }

        @Override public Object getPrincipal() { return getName(); }

        @Override public boolean isAuthenticated() { return true; }

        @Override
        public void setAuthenticated(boolean isAuthenticated) {
            // No-op: This principal is used internally for token requests and is always trusted
        }
        @Override public java.util.Collection<org.springframework.security.core.GrantedAuthority> getAuthorities() {
            // This Principal is used exclusively for Client Credentials Grant (service-to-service).
            // Since there is no end-user context, no specific authorities or roles are required.
            return List.of();
        }
    }
}
