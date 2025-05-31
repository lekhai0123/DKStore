package com.dkstore.services;

import java.util.List;

import org.springframework.data.domain.Page;

import com.dkstore.models.Product;
import com.dkstore.models.SanPhamTonKho;

public interface SanPhamTonKhoService {
	List<SanPhamTonKho> getAll();
	Boolean create(SanPhamTonKho sanPhamTonKho);
	SanPhamTonKho findById(Integer productId);
	Boolean update(SanPhamTonKho sanPhamTonKho);
	Boolean delete(Integer id);
	Page<SanPhamTonKho> getAll(Integer pageNo);
	Integer updateTonKho(Integer size, Integer productId, Integer oldSoLuong, Integer newSoLuong);
	Boolean deleteGioHangAndUpdateTonKho(Integer gioHangId);
}
