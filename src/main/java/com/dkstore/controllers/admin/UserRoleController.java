package com.dkstore.controllers.admin;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.dkstore.models.Role;
import com.dkstore.models.User;
import com.dkstore.models.UserRole;
import com.dkstore.repository.RoleRepository;
import com.dkstore.repository.UserRepository;
import com.dkstore.services.UserRoleService;

@Controller
@RequestMapping("/admin")
public class UserRoleController {
	@Autowired
	private UserRoleService userRoleService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private RoleRepository roleRepository;
	@GetMapping("/author-user/{id}")
	public String index(@PathVariable Integer id, Model model) {
		Optional<User> user = userRepository.findById(id);
		UserRole userRole = userRoleService.findById(id);
		if(userRole == null) {
			userRole = new UserRole();
		}
		model.addAttribute("user",user.orElse(null));
		model.addAttribute("userRole",userRole);
		model.addAttribute("idUser", id);
		return "admin/user/author";
	}
	@PostMapping("/author-user")
	public String confirm(@ModelAttribute UserRole userRole,@RequestParam Integer idUser,@RequestParam Integer role) {
		Optional<User> userOptional = this.userRepository.findById(idUser);
		if (userOptional.isPresent()) {
		    userRole.setUser(userOptional.get());  // Nếu có giá trị, lấy User và gọi setUser
		} else {
		    // Xử lý trường hợp không tìm thấy User, ví dụ gán null hoặc thông báo lỗi
		    userRole.setUser(null); // Hoặc xử lý khác tùy theo yêu cầu
		}
		Optional<Role> roleOptional = this.roleRepository.findById(Long.valueOf(role));
		if (roleOptional.isPresent()) {
			userRole.setRole(roleOptional.get());  // Nếu có giá trị, lấy User và gọi setUser
		} else {
			// Xử lý trường hợp không tìm thấy User, ví dụ gán null hoặc thông báo lỗi
			userRole.setRole(null); // Hoặc xử lý khác tùy theo yêu cầu
		}

		if (this.userRoleService.update(userRole)) {
			return "redirect:/admin/user";
		} else {
			return "redirect:/admin/author-user/" + userRole.getUser().getId();
		}
	}
}
