package com.sirioitalia.api.security;

import com.sirioitalia.api.security.filter.CustomAuthenticationFilter;
import com.sirioitalia.api.security.filter.CustomCorsFilter;
import com.sirioitalia.api.service.UserService;
import com.sirioitalia.api.util.PBKDF2PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
    private final UserDetailsService userDetailsService;
    private final PBKDF2PasswordEncoder pbkdf2PasswordEncoder;
    private final UserService userService;

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(pbkdf2PasswordEncoder);
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        CustomAuthenticationFilter customAuthenticationFilter = new CustomAuthenticationFilter(authenticationManagerBean(), userService);
        customAuthenticationFilter.setFilterProcessesUrl("/auth/login");
        customAuthenticationFilter.setUsernameParameter("email");
        customAuthenticationFilter.setPasswordParameter("password");

        http
                .cors().and()
                .csrf().disable()
                .anonymous().disable()
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll();
        http.addFilter(customAuthenticationFilter);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Content-Type, Access-Control-Allow-Origin, " +
                "Access-Control-Allow-Headers, Authorization, X-Requested-With, requestId, Correlation-Id"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/*", configuration);

        return source;
    }

    @Bean
    public FilterRegistrationBean<CustomCorsFilter> filterRegistrationBean() {
        FilterRegistrationBean<CustomCorsFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CustomCorsFilter());
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
