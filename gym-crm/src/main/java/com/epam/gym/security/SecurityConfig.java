package com.epam.gym.security;

import com.epam.gym.web.logging.TransactionIdFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * Two stateless filter chains:
 * <ul>
 *     <li>{@code GET /api/auth/login} authenticates the username and password from the HTTP Basic header
 *     (with brute force protection) and the controller answers with a JWT;</li>
 *     <li>every other endpoint requires that JWT as {@code Authorization: Bearer <token>}, except
 *     trainee/trainer registration, API docs and public actuator endpoints.
 *     {@code POST /api/auth/logout} revokes the token.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final String LOGIN_URL = "/api/auth/login";
    public static final String LOGOUT_URL = "/api/auth/logout";

    private static final String[] PUBLIC_URLS = {
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
            "/actuator/health/**", "/actuator/info", "/actuator/prometheus",
            "/error"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt generates a random salt per password and stores it inside the hash
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher publisher) {
        return new DefaultAuthenticationEventPublisher(publisher);
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder,
                                                       AuthenticationEventPublisher eventPublisher) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        ProviderManager manager = new ProviderManager(provider);
        manager.setAuthenticationEventPublisher(eventPublisher);
        return manager;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain loginFilterChain(HttpSecurity http,
                                                AuthenticationManager authenticationManager,
                                                SecurityErrorWriter errorWriter,
                                                LoginAttemptService loginAttemptService) throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(errorWriter, loginAttemptService,
                "Basic realm=\"gym-crm\"", "Missing or invalid Authorization header");
        applyStatelessDefaults(http)
                .securityMatcher(LOGIN_URL)
                .authenticationManager(authenticationManager)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http,
                                              AuthenticationManager authenticationManager,
                                              JwtService jwtService,
                                              TokenBlacklist tokenBlacklist,
                                              SecurityErrorWriter errorWriter,
                                              LoginAttemptService loginAttemptService) throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(errorWriter, loginAttemptService,
                "Bearer realm=\"gym-crm\"", "Missing bearer token");
        applyStatelessDefaults(http)
                .authenticationManager(authenticationManager)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/trainees", "/api/trainers").permitAll()
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, tokenBlacklist), LogoutFilter.class)
                .logout(logout -> logout
                        .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, LOGOUT_URL))
                        .addLogoutHandler(new JwtLogoutHandler(tokenBlacklist))
                        .logoutSuccessHandler(new RestLogoutSuccessHandler(entryPoint)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(new RestAccessDeniedHandler(errorWriter)));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${gym.security.cors.allowed-origins}") List<String> allowedOrigins,
            @Value("${gym.security.cors.max-age}") Duration maxAge) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins.stream().map(String::trim).filter(o -> !o.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, TransactionIdFilter.HEADER));
        configuration.setExposedHeaders(List.of(TransactionIdFilter.HEADER, HttpHeaders.WWW_AUTHENTICATE,
                HttpHeaders.RETRY_AFTER));
        // Tokens travel in the Authorization header, not in cookies
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static HttpSecurity applyStatelessDefaults(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                // No cookies or sessions are used, so there is nothing for CSRF to protect
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }
}
