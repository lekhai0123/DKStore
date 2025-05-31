package com.dkstore.services;

import java.util.List;

import org.springframework.data.domain.Page;

import com.dkstore.models.Product;

public interface ProductService {
	List<Product> getAll();
	Boolean create(Product product);
	Product findById(Integer productId);
	Boolean update(Product product);
	Boolean delete(Integer id);
	void setMainImageForProduct(Integer productId);
	Page<Product> getAll(Integer pageNo,Integer size);
	List<Product> search(String keyword);
	Page<Product> search(String keyword, Integer pageNo,Integer size);
	Page<Product> searchAndFilter(String keyword, List<String> brands, Integer pageNo);
	List<Product> getTop6Products();
	List<Product> getTop4Products();
}
