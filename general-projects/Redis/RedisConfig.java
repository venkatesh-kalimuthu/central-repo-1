package com.ford.decisionplatform.utils;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${rdso_memstore_Secret}")
    private RedisPassword redisPassword;

    @Value("${rdso_memstore_ca_certificate}")
    private String serverCA;

    @SneakyThrows
    @Bean
    public TrustManagerFactory trustManagerFactory() {

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        InputStream certIS = new ByteArrayInputStream(serverCA.getBytes(StandardCharsets.UTF_8));
        trustStore.setCertificateEntry("serverCA", CertificateFactory.getInstance("X509").generateCertificate(certIS));

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        return trustManagerFactory;
    }

    @Bean
    public RedisStandaloneConfiguration redisStandaloneConfiguration() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(redisHost, redisPort);
        redisStandaloneConfiguration.setPassword(redisPassword);
        return redisStandaloneConfiguration;
    }

    @Bean
    public RedisConnectionFactory connectionFactory() {

        SslOptions sslOptions = SslOptions.builder().trustManager(trustManagerFactory())
                .jdkSslProvider()
                .build();

        ClientOptions clientOptions = ClientOptions.builder().sslOptions(sslOptions).build();

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigurationBuilder = LettuceClientConfiguration.builder();

        LettuceClientConfiguration clientConfiguration = clientConfigurationBuilder
                .clientOptions(clientOptions)
                .useSsl()
                .build();

        return new LettuceConnectionFactory(redisStandaloneConfiguration(), clientConfiguration);
    }
}
