package com.dkstore.services;

import java.util.List;

import org.springframework.data.domain.Page;

import com.dkstore.models.GioHang;

public interface GioHangService {
	List<GioHang> getAll();
	Boolean create(GioHang giohang);
	GioHang findById(Integer id);
	Boolean update(GioHang giohang);
	Boolean delete(Integer id);
	Page<GioHang> getAll(Integer pageNo);
	List<GioHang> search(String keyword);
	Page<GioHang> search(String keyword, Integer pageNo);
}
