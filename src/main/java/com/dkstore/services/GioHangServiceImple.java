package com.dkstore.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.dkstore.models.GioHang;
import com.dkstore.models.Product;
import com.dkstore.repository.GioHangRepository;

@Service
public class GioHangServiceImple implements GioHangService{
	@Autowired
	private GioHangRepository gioHangRepository;
	
	@Override
	public List<GioHang> getAll() {
		// TODO Auto-generated method stub
		return this.gioHangRepository.findAll();
	}

	@Override
	public Boolean create(GioHang giohang) {
		// TODO Auto-generated method stub
		try {
			this.gioHangRepository.save(giohang);
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public GioHang findById(Integer id) {
		// TODO Auto-generated method stub
		return this.gioHangRepository.findById(id).get();
	}

	@Override
	public Boolean update(GioHang giohang) {
		// TODO Auto-generated method stub
	    if (giohang.getId() != null) {
	        this.gioHangRepository.save(giohang);
	        return true; 
	    }
	    return false;
	}

	@Override
	public Boolean delete(Integer id) {
		// TODO Auto-generated method stub
		try {
			this.gioHangRepository.delete(findById(id));
			;
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public Page<GioHang> getAll(Integer pageNo) {
		// TODO Auto-generated method stub
		Pageable pageable = PageRequest.of(pageNo - 1, 5);
		return this.gioHangRepository.findAll(pageable);
	}

	@Override
	public List<GioHang> search(String keyword) {
		// TODO Auto-generated method stub
		return this.gioHangRepository.search(keyword);
	}

	@Override
	public Page<GioHang> search(String keyword, Integer pageNo) {
		// TODO Auto-generated method stub
		List<GioHang> list = this.search(keyword);
		Pageable pageable = PageRequest.of(pageNo - 1, 5);
		Integer start = (int) pageable.getOffset();
		Integer end = (int) ((pageable.getOffset() + pageable.getPageSize()) > list.size() ? list.size()
				: pageable.getOffset() + pageable.getPageSize());
		list = list.subList(start, end);
		return new PageImpl<GioHang>(list, pageable, this.search(keyword).size());
	}

}
