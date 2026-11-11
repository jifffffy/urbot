package com.embabel.urbot.security;

import com.embabel.agent.rag.service.NamedEntityDataRepository;
import com.embabel.urbot.user.DummyUrbotUserService;
import com.embabel.urbot.user.UrbotUser;
import com.embabel.urbot.user.UrbotUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Spring Security configuration for Urbot.
 */
@Configuration
@EnableWebSecurity
class SecurityConfiguration {

    private final UrbotUserService userService;

    SecurityConfiguration(@Lazy UrbotUserService userService) {
        this.userService = userService;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
        );

        http.userDetailsService(userService);
        http.formLogin(Customizer.withDefaults());

        // Disable the request cache to prevent phantom session creation.
        // When a stale request (from a previous server run or expired connection)
        // arrives with an invalid JSESSIONID, Spring Security's default
        // HttpSessionRequestCache calls getSession() to save the request, which
        // creates a new empty session and sets a Set-Cookie that overwrites
        // the authenticated session cookie.
        http.requestCache(cache -> cache.requestCache(new NullRequestCache()));

        http.logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .logoutRequestMatcher(PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/logout"))
                .permitAll()
        );

        return http.build();
    }

    @Bean
    UrbotUserService userService(NamedEntityDataRepository entityRepository) {
        return new DummyUrbotUserService(entityRepository,
                new UrbotUser("1", "Alice Agu", "alice"),
                new UrbotUser("2", "Ben Blossom", "ben")
        );
    }
}
