package com.dkstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dkstore.models.Role;

public interface RoleRepository extends JpaRepository<Role, Long>{
	Role findByName(String name);
}
