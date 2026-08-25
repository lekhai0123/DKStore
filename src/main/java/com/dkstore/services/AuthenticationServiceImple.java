package com.dkstore.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.dkstore.models.ConfirmationToken;
import com.dkstore.models.Role;
import com.dkstore.models.User;
import com.dkstore.models.UserRole;
import com.dkstore.repository.ConfirmationTokenRepository;
import com.dkstore.repository.RoleRepository;
import com.dkstore.repository.UserRepository;

@Service
public class AuthenticationServiceImple implements AuthenticationService{

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConfirmationTokenRepository confirmationTokenRepository;

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private RoleRepository roleRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    public String registerUser(User user) {
        // Lưu người dùng với trạng thái chưa được kích hoạt (isEnabled = false)
        user.setEnabled(false); // Chưa xác thực email
        user.setPassWord(new BCryptPasswordEncoder().encode(user.getPassWord()));
        Role userRole = roleRepository.findByName("USER");
		if (userRole != null) {
	        UserRole userRoleEntity = new UserRole();
	        userRoleEntity.setUser(user);
	        userRoleEntity.setRole(userRole);
	        
	        // Gán UserRole cho User
	        user.getUserRoles().add(userRoleEntity);
	    }
        userRepository.save(user); 

        // Tạo token xác thực
        String token = UUID.randomUUID().toString();
        ConfirmationToken confirmationToken = new ConfirmationToken(token, user, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        confirmationTokenRepository.save(confirmationToken);

        // Gửi email xác thực
        sendConfirmationEmail(user, token);

        return "Đã gửi email xác thực. Vui lòng kiểm tra hộp thư.";
    }
    private void sendConfirmationEmail(User user, String token) {
        String subject = "Xác thực tài khoản của bạn";
        String confirmationUrl = baseUrl + "/confirm?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText("Vui lòng xác thực tài khoản của bạn bằng cách nhấp vào liên kết sau: " + confirmationUrl);

        mailSender.send(message);
    }
    public String confirmToken(String token) {
        ConfirmationToken confirmationToken = confirmationTokenRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Token xác thực không hợp lệ"));

        if (confirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token xác thực đã hết hạn");
        }

        User user = confirmationToken.getUser();
        user.setEnabled(true); // Kích hoạt tài khoản
        userRepository.save(user);

        return "Tài khoản của bạn đã được xác thực. Bạn có thể đăng nhập.";
    }
    public String resendConfirmationEmail(User user) {
        String token = UUID.randomUUID().toString();
        ConfirmationToken confirmationToken = new ConfirmationToken(token, user, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        confirmationTokenRepository.save(confirmationToken);
        
        sendConfirmationEmail(user, token); // Gửi lại email xác thực
        return "Đã gửi lại email xác thực. Vui lòng kiểm tra hộp thư.";
    }
    public void processForgotPassword(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));

        String token = UUID.randomUUID().toString();
        ConfirmationToken confirmationToken = new ConfirmationToken(token, user, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        confirmationTokenRepository.save(confirmationToken);

        sendResetPasswordEmail(user, token);
    }

    private void sendResetPasswordEmail(User user, String token) {
        String subject = "Khôi phục mật khẩu của bạn";
        String resetPasswordUrl = baseUrl + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText("Vui lòng nhấp vào liên kết sau để khôi phục mật khẩu của bạn: " + resetPasswordUrl);

        mailSender.send(message);
    }
}
