package com.server.realsync.config;

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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.server.realsync.services.UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private UserService userService;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomAuthenticationSuccessHandler successHandler,
			CustomAuthenticationFailureHandler failureHandler, CustomLogoutSuccessHandler customLogoutSuccessHandler)
			throws Exception {

		http.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/accounts/signup", "/api/accounts/check-email",
    "/api/accounts/check-mobile","/mweb/login", "/signup.html", "/register.html",
								"/login", "/privacy",
								"/terms", "/css/**", "/js/**", "/img/**", "/assets/**", "/realsync-assets/**",
								"/promo/**", "/api/promotions/public/**",
								"/api/public/invoices/**", "/i/**", "/invoice-view/**")
						.permitAll().requestMatchers("/").permitAll()
						.requestMatchers("/register").permitAll()
						.requestMatchers("/register.html").permitAll()
						.requestMatchers(new AntPathRequestMatcher("/realsync-assets/**")).permitAll()
						.requestMatchers("/realsync/**").permitAll().requestMatchers("/mweb/register").permitAll()
						.requestMatchers("/mweb/terms").permitAll().requestMatchers("/api/auth/register").permitAll()
						.requestMatchers(new AntPathRequestMatcher("/**/invoice/**")).permitAll()
						.requestMatchers(new AntPathRequestMatcher("/**/appointment/**")).permitAll()
						.requestMatchers("/register").permitAll().anyRequest().authenticated())
				.formLogin(form -> form
						.loginPage("/login.html") // GET /login (login page)
						.loginProcessingUrl("/login") // POST /login (handled by Spring Security)
						.defaultSuccessUrl("/home.html", true)
						.failureUrl("/login.html?error=true")
						.permitAll())

				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/login.html?logout")
						.permitAll())
				// ✅ Persistent login configuration
				.rememberMe(remember -> remember.key("uniqueAndSecret") // A secret key used to sign cookies
						.tokenValiditySeconds(60 * 60 * 24 * 14) // 14 days validity
						.userDetailsService(userService))
				.sessionManagement(session -> session.sessionFixation().migrateSession()
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
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

}