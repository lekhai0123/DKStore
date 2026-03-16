package com.dkstore.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.dkstore.models.HinhAnhSanPham;
import com.dkstore.models.Product;
import com.dkstore.repository.HinhAnhSanPhamRepository;
import com.dkstore.repository.ProductRepository;

@Service
public class ProductServiceImple implements ProductService {

	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private HinhAnhSanPhamRepository hinhAnhSanPhamRepository;

	@Override
	public List<Product> getAll() {
		// TODO Auto-generated method stub
		return this.productRepository.findAll();
	}

	@Override
	public Boolean create(Product product) {
		// TODO Auto-generated method stub
		try {
			this.productRepository.save(product);
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public Product findById(Integer id) {
		return this.productRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
	}

	@Override
	public Boolean update(Product product) {
		// TODO Auto-generated method stub
		try {
			this.productRepository.save(product);
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
			this.productRepository.delete(findById(id));
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return false;
	}
	@Override
	public Page<Product> getAll(Integer pageNo,Integer size) {
		// TODO Auto-generated method stub
		Pageable pageable = PageRequest.of(pageNo - 1, size);
		return this.productRepository.findAll(pageable);
	}

	@Override
	public List<Product> search(String keyword) {
		// TODO Auto-generated method stub
		return this.productRepository.search(keyword);
	}


	@Override
	public Page<Product> search(String keyword, Integer pageNo,Integer size) {
		// TODO Auto-generated method stub
		List<Product> list = this.search(keyword);
		Pageable pageable = PageRequest.of(pageNo - 1, size);
		Integer start = (int) pageable.getOffset();
		Integer end = (int) ((pageable.getOffset() + pageable.getPageSize()) > list.size() ? list.size()
				: pageable.getOffset() + pageable.getPageSize());
		list = list.subList(start, end);
		return new PageImpl<Product>(list, pageable, this.search(keyword).size());
	}

	@Override
	public List<Product> getTop6Products() {
		// TODO Auto-generated method stub
		return productRepository.findTop6ByOrderByIdAsc();
	}

	@Override
	public List<Product> getTop4Products() {
		// TODO Auto-generated method stub
		return productRepository.findTop4ByOrderByIdDesc();
	}

	@Override
	public void setMainImageForProduct(Integer productId) {
        // Lấy sản phẩm theo ID
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Tìm hình ảnh chính của sản phẩm
        HinhAnhSanPham mainImage = hinhAnhSanPhamRepository.findByProductIdAndIsMainTrue(productId);

        if (mainImage != null) {
            // Cập nhật URL của hình ảnh chính vào thuộc tính mainImage của sản phẩm
            product.setMainImage(mainImage.getUrlImage());
            productRepository.save(product); // Lưu lại sản phẩm
        }
    }

	@Override
public Page<Product> searchAndFilter(String keyword, List<String> brands, Integer pageNo) {

    if (keyword != null) {
        keyword = keyword.trim();
        if (keyword.isEmpty()) {
            keyword = null;
        }
    }

    if (brands != null && brands.isEmpty()) {
        brands = null;
    }

    Pageable pageable = PageRequest.of(pageNo - 1, 9);

    return productRepository.findByKeywordAndBrands(keyword, brands, pageable);
}
}
