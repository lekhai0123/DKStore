package com.dkstore.controllers.admin;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.dkstore.models.ConfirmationToken;
import com.dkstore.models.Product;
import com.dkstore.models.Role;
import com.dkstore.models.User;
import com.dkstore.models.UserRole;
import com.dkstore.repository.ConfirmationTokenRepository;
import com.dkstore.repository.RoleRepository;
import com.dkstore.repository.UserRepository;
import com.dkstore.services.AuthenticationService;
import com.dkstore.services.AuthenticationServiceImple;
import com.dkstore.services.UserService;

@Controller
public class UserController {
	@Autowired
	private UserService userService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private AuthenticationService authenticationService;
	@Autowired
	private AuthenticationServiceImple authenticationServiceImple;
	@Autowired
	private ConfirmationTokenRepository confirmationTokenRepository;
	@Autowired
	private RoleRepository roleRepository;
	@GetMapping("/admin/user")
	public String index(Model model, @RequestParam(defaultValue = "1") Integer pageNo,@Param("keyword") String keyword) {
		Page<User> listUsers = this.userService.getAll(pageNo);
		if(keyword != null) {
			listUsers = this.userService.search(keyword,pageNo);
			model.addAttribute("keyword",keyword);
		}
		model.addAttribute("userLists", listUsers);
		model.addAttribute("totalPage", listUsers.getTotalPages());
		model.addAttribute("currentPage", pageNo);
		return "admin/user/index";
	}
	@GetMapping("/login")	
	public String login(@RequestParam (required = false) String error,Model model) {
		if (error != null) {
            if (error.equals("empty-fields")) {
                model.addAttribute("errorMessage", "Vui lòng nhập cả tên người dùng và mật khẩu.");
            } else if (error.equals("invalid-credentials")) {
                model.addAttribute("errorMessage", "Tên người dùng hoặc mật khẩu không chính xác.");
            }
        }
		return "admin/login";
	}
	@GetMapping("/resend-email/{username}")
	public String resendEmail(@PathVariable String username, Model model) {
	    User user = userRepository.findByUserName(username);
	    if (user != null) {
	        String response = authenticationServiceImple.resendConfirmationEmail(user);
	        model.addAttribute("message", response);
	        model.addAttribute("username",username);
	        return "admin/authentication/required";
	    }
	    model.addAttribute("error", "Không tìm thấy người dùng");
	    return "admin/login";
	}
	@GetMapping("/account-not-confirmed")
	public String accountNotConfirmed(@RequestParam String username,Model model) {
		model.addAttribute("username",username);
	    return "admin/authentication/loginFail"; // Trang thông báo tài khoản chưa xác thực
	}
	@PostMapping("/forgot-password")
	public String forgotPassword(@RequestParam String email, Model model) {
	    authenticationServiceImple.processForgotPassword(email);
	    model.addAttribute("email", email);
	    return "admin/authentication/resetpassword";
	}
	@GetMapping("/reset-password")
	public String resetPassword(@RequestParam String token, Model model) {
	    ConfirmationToken confirmationToken = confirmationTokenRepository.findByToken(token)
	        .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ"));

	    if (confirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
	        throw new IllegalArgumentException("Token đã hết hạn");
	    }

	    model.addAttribute("token", token);
	    return "admin/newpassword"; // Chuyển tới form tạo mật khẩu mới
	}
	@PostMapping("/reset-password")
	public String resetPassword(@RequestParam String token, @RequestParam String newPassword, Model model) {
	    ConfirmationToken confirmationToken = confirmationTokenRepository.findByToken(token)
	        .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ"));
	    if (confirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
	        throw new IllegalArgumentException("Token đã hết hạn");
	    }
	    User user = confirmationToken.getUser();
	    System.out.println(newPassword);
	    user.setPassWord(new BCryptPasswordEncoder().encode(newPassword));
	    userRepository.save(user);
	    return "admin/authentication/resetsuccessfully";
	}
	@GetMapping("/admin/add-user")
	public String add(Model model) {
		User user = new User();
		model.addAttribute("user",user);
		return "admin/user/add";
	}
	@PostMapping("/admin/add-user")
	public String save(@ModelAttribute User user,RedirectAttributes redirectAttributes) {
		BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
		user.setPassWord(bCryptPasswordEncoder.encode(user.getPassWord()));
		Role userRole = roleRepository.findByName("USER");
		if (userRole != null) {
	        UserRole userRoleEntity = new UserRole();
	        userRoleEntity.setUser(user);
	        userRoleEntity.setRole(userRole);
	        
	        // Gán UserRole cho User
	        user.getUserRoles().add(userRoleEntity);
	    }
		if (this.userService.create(user)) {
			redirectAttributes.addFlashAttribute("successMessage","Đã đăng ký thành công");
			return "redirect:/admin/user";
		} else {
			redirectAttributes.addFlashAttribute("errorMessage","Đã xảy ra lỗi trong quá trình đăng ký");
			return "admin/user/add";
		}
	}
	@GetMapping("/admin/edit-user/{username}")
	public String edit(Model model,@PathVariable String username) {
		User user = this.userService.findByUserName(username);
		model.addAttribute("user",user);
		return "admin/user/edit";
	}
	@PostMapping("/admin/edit-user")
	public String update(@ModelAttribute User user) {
		if (this.userService.update(user)) {
			return "redirect:/admin/user";
		} else {
			return "admin/brand/edit/" + user.getUserName();
		}
	}
	@GetMapping("/admin/delete-user/{username}")
	public String delete(@PathVariable String username) {
		if (this.userService.delete(username)) {
			return "redirect:/admin/user";
		} else {
			return "admin/user/index";
		}
	}
    @PostMapping("/register")
    public ModelAndView register(@ModelAttribute User user) {
        // Gọi service để đăng ký người dùng
        String response = authenticationService.registerUser(user);
        
        // Tạo đối tượng ModelAndView để trả về trang xác nhận
        ModelAndView modelAndView = new ModelAndView("admin/authentication/required");
        modelAndView.addObject("message", response); // Thêm thông điệp từ service vào view
        modelAndView.addObject("username",user.getUserName());
        return modelAndView; // Trả về view
    }

    @GetMapping("/confirm")
    public String confirm(@RequestParam String token,Model model) {
        try {
            authenticationService.confirmToken(token);
            // Trả về trang HTML thành công nếu không có lỗi
            return "admin/authentication/confirmationSuccess";
        } catch (IllegalArgumentException e) {
            // Nếu có lỗi, thêm thông báo lỗi vào model
            model.addAttribute("error", e.getMessage());
            // Trả về trang HTML hiển thị lỗi
            return "admin/authentication/confirmationError";
        }
    }
	@PostMapping("/check-username")
	@ResponseBody
	public String checkUserName(@RequestParam String userName) {
	    if (userRepository.existsByUserName(userName)) {
	        return "username_exists";
	    }
	    return "username_available";
	}
	@PostMapping("/check-email")
	@ResponseBody
	public String checkEmail(@RequestParam String email) {
		if (userRepository.existsByEmail(email)) {
	        return "email_exists";
	    }
	    return "email_available";
	}
}