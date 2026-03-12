package com.ford.riskmonitoring.observabilitybeamdataflow.security;

import com.ford.cloudnative.base.app.security.resourceserver.JwtRolesAndScopesConverter;
import com.ford.cloudnative.base.app.web.exception.handler.servlet.WebSecurityResponseExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfiguration {

	/*****************************************************************************************************************
	 * Resource Server (handle incoming Bearer tokens)
	 *****************************************************************************************************************/

	@Configuration
	public static class ResourceServerSecurityConfiguration {

		JwtDecoder jwtDecoder;

		WebSecurityResponseExceptionHandler exceptionHandler;

		@Value("${api.endpoints.hello.v1.admin-role}")
		String helloV1AdminRole;

		@Value("${api.endpoints.messages.v1.admin-role}")
		String messagesV1AdminRole;

		@Autowired
		public ResourceServerSecurityConfiguration(JwtDecoder decoratedJwtDecoder, WebSecurityResponseExceptionHandler exceptionHandler) {
			this.exceptionHandler = exceptionHandler;
			this.jwtDecoder = decoratedJwtDecoder;
		}

		@Bean
		@Order(10)
		public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {

			http
				.securityMatcher("/api/**")
				.csrf(AbstractHttpConfigurer::disable)
				.headers(headers ->
						headers
								.defaultsDisabled()
								.addHeaderWriter((request, response) -> response.setHeader("Cross-Origin-Opener-Policy", "same-origin")))
				.authorizeHttpRequests(authorizeHttpRequest -> {
					// Hello API
					authorizeHttpRequest.requestMatchers(
							antMatcher("/api/v1/hello/**")
					).hasAnyRole(helloV1AdminRole);

					// Messages API
					authorizeHttpRequest.requestMatchers(
							antMatcher("/api/v1/messages/**")
					).hasAnyRole(messagesV1AdminRole);

					// Others
					authorizeHttpRequest.anyRequest().authenticated();
				})
				.sessionManagement(sessionManagement ->
						sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.oauth2ResourceServer(oauth2ResourceServer -> {
					oauth2ResourceServer.accessDeniedHandler(exceptionHandler);
					oauth2ResourceServer.authenticationEntryPoint(exceptionHandler);
					oauth2ResourceServer.jwt(jwt -> {
						jwt.decoder(jwtDecoder);
						jwt.jwtAuthenticationConverter(new JwtRolesAndScopesConverter());
					});
				});

			return http.build();
		}
	}


	/*****************************************************************************************************************
	 * Other - Basic/Public
	 *****************************************************************************************************************/

	@Configuration
	public static class HttpSecurityConfiguration {

		@Bean
		@Order(30)
		public SecurityFilterChain httpSecurityFilterChain(HttpSecurity http) throws Exception {
			http
				.csrf(AbstractHttpConfigurer::disable)
				.headers(headers ->
						headers
							.addHeaderWriter((request, response) -> response.setHeader("Cross-Origin-Opener-Policy", "same-origin")))
				.authorizeHttpRequests(authorizeHttpRequest -> {
					authorizeHttpRequest.requestMatchers(
							antMatcher("/"),
							antMatcher("/csrf"),
							antMatcher("/error"),
							antMatcher("/favicon.ico"),
							antMatcher("/health/**"),
							antMatcher("/swagger-ui/**"),
							antMatcher("/swagger-*"),
							antMatcher("/swagger-resources/**"),
							antMatcher("/webjars/**"),
							antMatcher("/v3/api-docs"),
							antMatcher("/v3/api-docs/**"),
							antMatcher("/api/**")
					).permitAll();
					authorizeHttpRequest.requestMatchers(EndpointRequest.to("info", "health", "refresh", "startup")).permitAll();
					authorizeHttpRequest.anyRequest().authenticated();
				})
				.sessionManagement(sessionManagement ->
						sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(withDefaults());

			return http.build();
		}
	}

	@Bean
	public InMemoryUserDetailsManager inMemoryUserDetailsManager(
			SecurityProperties properties, ObjectProvider<PasswordEncoder> passwordEncoder) {
		return new UserDetailsServiceAutoConfiguration().inMemoryUserDetailsManager(properties, passwordEncoder);
	}
}
