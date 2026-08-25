package com.dkstore.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.dkstore.models.ChiTietHoaDon;
import com.dkstore.models.HoaDon;
import com.dkstore.models.Product;
import com.dkstore.repository.HoaDonRepository;

import jakarta.transaction.Transactional;

@Service
public class HoaDonServiceImple implements HoaDonService{
	@Autowired
	private HoaDonRepository hoaDonRepository;
	@Autowired
	private ChiTietHoaDonService chiTietHoaDonService;
	@Override
	public List<HoaDon> getAll() {
		// TODO Auto-generated method stub
		return this.hoaDonRepository.findAll();
	}

	@Override
	public Boolean create(HoaDon hoadon) {
		// TODO Auto-generated method stub
		try {
			this.hoaDonRepository.save(hoadon);
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public HoaDon findById(Integer id) {
		// TODO Auto-generated method stub
		return this.hoaDonRepository.findById(id).get();
	}

	@Override
	@Transactional
	public Boolean update(HoaDon hoaDon) {
	    if (hoaDon.getId() != null) {
	        this.hoaDonRepository.save(hoaDon);
	        return true; 
	    }
	    return false;
	}

	@Override
	public Boolean delete(Integer id) {
	    try {
	        HoaDon hoadon = findById(id);
	        if (hoadon != null) {
	            for (ChiTietHoaDon chiTiet : hoadon.getChiTietHoaDon()) {
	                chiTietHoaDonService.delete(chiTiet.getId()); 
	            }
	            this.hoaDonRepository.delete(hoadon);
	            return true;
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return false;
	}

	@Override
	public Page<HoaDon> getAll(Integer pageNo) {
		// TODO Auto-generated method stub
		Pageable pageable = PageRequest.of(Math.max(pageNo - 1, 0), 5);
		return this.hoaDonRepository.findAll(pageable);
	}

	@Override
	public List<HoaDon> search(String keyword) {
		// TODO Auto-generated method stub
		return this.hoaDonRepository.search(keyword);
	}

	@Override
	public Page<HoaDon> search(String keyword, Integer pageNo) {
		// TODO Auto-generated method stub
		List<HoaDon> list = this.search(keyword);
		Pageable pageable = PageRequest.of(Math.max(pageNo - 1, 0), 5);
		Integer start = (int) pageable.getOffset();
		Integer end = (int) ((pageable.getOffset() + pageable.getPageSize()) > list.size() ? list.size()
				: pageable.getOffset() + pageable.getPageSize());
		list = list.subList(start, end);
		return new PageImpl<HoaDon>(list, pageable, this.search(keyword).size());
	}

}
