package com.dkstore.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.dkstore.models.HoaDon;
import com.dkstore.models.User;

public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> {
    @Query("SELECT h FROM HoaDon h JOIN h.user u WHERE LOWER(u.userName) LIKE LOWER(CONCAT('%', ?1, '%'))")
    List<HoaDon> search(String keyword);

    List<HoaDon> findByUser(User user);
}