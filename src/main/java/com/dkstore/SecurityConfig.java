package com.dkstore;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.dkstore.models.User;
import com.dkstore.repository.UserRepository;
import com.dkstore.services.CustomUserDetailService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	@Autowired
	CustomUserDetailService customUserDetailService;
	@Autowired
	private UserRepository userRepository;

	@Bean
	BCryptPasswordEncoder passwordEncode() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
	    return http.csrf(csrf -> csrf.disable())
	            .authorizeHttpRequests(auth -> auth
	                    .requestMatchers("/admin", "/admin/**").hasAuthority("ADMIN")  // Chỉ cho phép người dùng có quyền ADMIN vào /admin
	            		.requestMatchers("/*","/shop/**").permitAll()  // Cho phép tất cả mọi người truy cập vào các trang khác	                    
	            		.requestMatchers("/user", "/user/**").hasAnyAuthority("USER", "ADMIN")
	                    .anyRequest().authenticated())  // Các yêu cầu khác yêu cầu người dùng đã đăng nhập
	            .formLogin(login -> login
	                    .loginPage("/login")  // Đặt trang đăng nhập
	                    .loginProcessingUrl("/login")  // URL xử lý đăng nhập
	                    .usernameParameter("username")  // Tham số username
	                    .passwordParameter("password")  // Tham số password
	                    .defaultSuccessUrl("/admin", true)  // Chuyển hướng đến /admin nếu đăng nhập thành công
	                    .failureHandler((request, response, exception) -> {  // Xử lý khi đăng nhập thất bại
	                        String username = request.getParameter("username");
	                        String password = request.getParameter("password");
	                        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
	                            response.sendRedirect("/login?error=empty-fields");
	                        } else {
	                            User user = userRepository.findByUserName(username); 
	                            if (user == null) {
	                                response.sendRedirect("/login?error=invalid-credentials");
	                            } else {
	                                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	                                if (!passwordEncoder.matches(password, user.getPassWord())) {
	                                    response.sendRedirect("/login?error=invalid-credentials");
	                                } else {
	                                    response.sendRedirect("/admin");
	                                }
	                            }
	                        }
	                    })
	                    .successHandler(authenticationSuccessHandler())  // Sử dụng AuthenticationSuccessHandler tùy chỉnh
	            )
	            .logout(logout -> logout.logoutUrl("/admin-logout").invalidateHttpSession(true).clearAuthentication(true).logoutSuccessUrl("/login").permitAll())  // Đăng xuất và chuyển hướng về trang login
	            .userDetailsService(customUserDetailService)  // Sử dụng custom UserDetailsService
	            .build();
	}

	@Bean
	public AuthenticationSuccessHandler authenticationSuccessHandler() {
	    return new AuthenticationSuccessHandler() {
	        @Override
	        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
	            String username = authentication.getName();
	            User user = userRepository.findByUserName(username);
	            if (user != null && !user.getEnabled()) {
	                response.sendRedirect("/account-not-confirmed?username=" + username);
	                return;
	            }
	            request.getSession().setAttribute("userId", user.getId());
	            if (authentication.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ADMIN"))) {
	                response.sendRedirect("/admin");
	            } else if (authentication.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("USER"))) {
	                response.sendRedirect("/");
	            } else {
	                response.sendRedirect("/login");
	            }
	        }
	    };
	}
	@Bean
	WebSecurityCustomizer webSecurityCustomizer() {
		return (web) -> web.ignoring().requestMatchers("/fe/**", "/assets/**", "/uploads/**");
	}
}
