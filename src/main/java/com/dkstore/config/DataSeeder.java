package com.dkstore.config;

import java.sql.Date;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.dkstore.models.Role;
import com.dkstore.models.User;
import com.dkstore.models.UserRole;
import com.dkstore.repository.RoleRepository;
import com.dkstore.repository.UserRepository;
import com.dkstore.repository.UserRoleRepository;

@Component
public class DataSeeder implements CommandLineRunner {

	private final RoleRepository roleRepository;
	private final UserRepository userRepository;
	private final UserRoleRepository userRoleRepository;
	private final PasswordEncoder passwordEncoder;

	public DataSeeder(
			RoleRepository roleRepository,
			UserRepository userRepository,
			UserRoleRepository userRoleRepository,
			PasswordEncoder passwordEncoder) {
		this.roleRepository = roleRepository;
		this.userRepository = userRepository;
		this.userRoleRepository = userRoleRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(String... args) {
		Role userRole = roleRepository.findByName("USER");
		if (userRole == null) {
			userRole = new Role();
			userRole.setName("USER");
			userRole = roleRepository.save(userRole);
		}

		Role adminRole = roleRepository.findByName("ADMIN");
		if (adminRole == null) {
			adminRole = new Role();
			adminRole.setName("ADMIN");
			adminRole = roleRepository.save(adminRole);
		}

		User user = userRepository.findByUserName("user");
		if (user == null) {
			user = new User();
			user.setUserName("user");
			user.setPassWord(passwordEncoder.encode("123456"));
			user.setEnabled(true);
			user.setFullName("Normal User");
			user.setGender("Nam");
			user.setBirthday(Date.valueOf("2003-01-01"));
			user.setAddress("Ho Chi Minh");
			user.setEmail("user@gmail.com");
			user.setTelephone("0900000001");
			user = userRepository.save(user);
		}

		User admin = userRepository.findByUserName("admin");
		if (admin == null) {
			admin = new User();
			admin.setUserName("admin");
			admin.setPassWord(passwordEncoder.encode("123456"));
			admin.setEnabled(true);
			admin.setFullName("Administrator");
			admin.setGender("Nam");
			admin.setBirthday(Date.valueOf("2003-01-01"));
			admin.setAddress("Ho Chi Minh");
			admin.setEmail("admin@gmail.com");
			admin.setTelephone("0900000002");
			admin = userRepository.save(admin);
		}

		if (!userRoleRepository.existsByUser_IdAndRole_Id(user.getId(), userRole.getId())) {
			UserRole ur = new UserRole();
			ur.setUser(user);
			ur.setRole(userRole);
			userRoleRepository.save(ur);
		}

		if (!userRoleRepository.existsByUser_IdAndRole_Id(admin.getId(), adminRole.getId())) {
			UserRole ur = new UserRole();
			ur.setUser(admin);
			ur.setRole(adminRole);
			userRoleRepository.save(ur);
		}
	}
}