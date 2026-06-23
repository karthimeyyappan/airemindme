package com.server.realsync.config;

import com.server.realsync.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserService userService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, 
                                                   CustomAuthenticationSuccessHandler successHandler,
                                                   CustomAuthenticationFailureHandler failureHandler, 
                                                   CustomLogoutSuccessHandler customLogoutSuccessHandler,
                                                   CustomOAuth2SuccessHandler oauthSuccessHandler, 
                                                   ClientRegistrationRepository clientRegistrationRepository) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/accounts/signup", "/api/accounts/check-email", "/api/accounts/check-mobile", 
                    "/mweb/login", "/signup.html", "/register.html", "/login", "/privacy", "/terms", 
                    "/css/**", "/js/**", "/img/**", "/assets/**", "/realsync-assets/**", "/promo/**", 
                    "/api/promotions/public/**", "/api/public/invoices/**", "/i/**", "/invoice-view/**", 
                    "/oauth2/**", "/login/oauth2/**", "/forgot-password.html", "/api/auth/forgot-password", 
                    "/no-account", "/", "/register", "/realsync/**", "/mweb/register", "/mweb/terms", 
                    "/api/auth/register", "/r/**", "/api/reports/public/report/**"
                ).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/**/invoice/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/**/appointment/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/realsync-assets/**")).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/home.html", true)
                .failureUrl("/login.html?error=true")
                .permitAll()
            )
            .oauth2Login(oauth -> oauth
                .loginPage("/login.html")
                .authorizationEndpoint(authorization -> authorization
                    .authorizationRequestResolver(promptSelectAccountResolver(clientRegistrationRepository)))
                .successHandler(oauthSuccessHandler)
                .failureHandler((request, response, exception) -> {
                    System.out.println("FAILURE HANDLER HIT");
                    exception.printStackTrace();
                    response.sendRedirect("/login.html?oauthFailure");
                })
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.html?logout")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("uniqueAndSecret")
                .tokenValiditySeconds(60 * 60 * 24 * 14) // 14 days
                .userDetailsService(userService)
            )
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .userDetailsService(userService);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> cookieCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {
            context.setSessionCookieName("JSESSIONID");
            context.setUseHttpOnly(true);
            context.setSessionTimeout(30); // minutes
        });
    }

    private OAuth2AuthorizationRequestResolver promptSelectAccountResolver(ClientRegistrationRepository repo) {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(customizer -> 
            customizer.additionalParameters(params -> params.put("prompt", "select_account"))
        );
        return resolver;
    }
}