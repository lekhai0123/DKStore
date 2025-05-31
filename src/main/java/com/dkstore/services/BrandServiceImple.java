package com.dkstore.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.dkstore.models.Brand;
import com.dkstore.repository.BrandRepository;

@Service
public class BrandServiceImple implements BrandService {
	@Autowired
	private BrandRepository brandRepository;

	@Override
	public List<Brand> getAll() {
		// TODO Auto-generated method stub
		return this.brandRepository.findAll();
	}

	@Override
	public Boolean create(Brand brand) {
		// TODO Auto-generated method stub
		try {
			this.brandRepository.save(brand);
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public Brand findById(Integer id) {
		// TODO Auto-generated method stub
		return this.brandRepository.findById(id).get();
	}

	@Override
	public Boolean update(Brand brand) {
		// TODO Auto-generated method stub
		try {
			this.brandRepository.save(brand);
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public Boolean delete(Integer id) {
		// TODO Auto-generated method stub
		try {
			this.brandRepository.delete(findById(id));
			;
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public List<Brand> search(String keyword) {
		// TODO Auto-generated method stub
		return this.brandRepository.search(keyword);
	}

	@Override
	public Page<Brand> getAll(Integer pageNo) {
		// TODO Auto-generated method stub
		Pageable pageable = PageRequest.of(pageNo - 1, 5);
		return this.brandRepository.findAll(pageable);
	}

	@Override
	public Page<Brand> search(String keyword, Integer pageNo) {
		// TODO Auto-generated method stub
		List<Brand> list = this.search(keyword);
		Pageable pageable = PageRequest.of(pageNo - 1, 5);
		Integer start = (int) pageable.getOffset();
		Integer end = (int) ((pageable.getOffset() + pageable.getPageSize()) > list.size() ? list.size()
				: pageable.getOffset() + pageable.getPageSize());
		list = list.subList(start, end);
		return new PageImpl<Brand>(list, pageable, this.search(keyword).size());
	}

}
