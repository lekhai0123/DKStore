package com.dkstore.controllers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.dkstore.models.Brand;
import com.dkstore.models.ChiTietGioHang;
import com.dkstore.models.GioHang;
import com.dkstore.models.HinhAnhSanPham;
import com.dkstore.models.Product;
import com.dkstore.models.User;
import com.dkstore.repository.GioHangRepository;
import com.dkstore.repository.ProductRepository;
import com.dkstore.services.BrandService;
import com.dkstore.services.GioHangService;
import com.dkstore.services.ProductServiceImple;
import com.dkstore.services.UserService;

import jakarta.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {
	@Autowired
	private ProductServiceImple productServiceImple;
	@Autowired
	private UserService userService;
	@Autowired
	private GioHangRepository gioHangRepository;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private BrandService brandService;

	@GetMapping("")
	public String home(Model model, HttpSession session) {
		List<Product> topProducts = productServiceImple.getTop6Products();
		List<Map<String, Object>> productList = topProducts.stream().map(product -> {
			Map<String, Object> data = new HashMap<>();

			BigDecimal gia = new BigDecimal(product.getPrice()).setScale(2, RoundingMode.HALF_UP);
			String giaFormatted = (gia.stripTrailingZeros().scale() <= 0) ? gia.toBigInteger().toString()
					: gia.toString();

			data.put("topProducts", product);
			data.put("gia", giaFormatted);
			return data;
		}).collect(Collectors.toList());
		model.addAttribute("productList", productList);
		Integer userId = (Integer) session.getAttribute("userId");
		if(userId != null) {
			User user = userService.findById(userId).get();
			List<GioHang> gioHang = gioHangRepository.findByUser(user);
			if (!gioHang.isEmpty()) {
				Set<ChiTietGioHang> chiTietGioHang = gioHang.get(0).getChiTietGioHang();
				Integer giohangId = gioHang.get(0).getId();
				model.addAttribute("chiTietGioHang", chiTietGioHang);
				model.addAttribute("giohangId", giohangId);
				Float totalPriceFloat = gioHang.get(0).getTongtien();
				BigDecimal totalPrice = new BigDecimal(totalPriceFloat).setScale(2, RoundingMode.HALF_UP);
				if (totalPrice.stripTrailingZeros().scale() <= 0) {
					totalPrice = totalPrice.setScale(0, RoundingMode.DOWN);
				}
				model.addAttribute("totalPrice", totalPrice);
			}
		}
		
		return "user/index";
	}

	@GetMapping("/shop")
	public String shop(Model model, HttpSession session,
			@RequestParam(value = "brand", required = false) List<String> brands,
			@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(required = false) String keyword) {
		List<Brand> listBrands = brandService.getAll();
		Page<Product> productPage = productServiceImple.searchAndFilter(keyword, brands, pageNo);
		Integer userId = (Integer) session.getAttribute("userId");
		if(userId != null) {

			User user = userService.findById(userId).get();
			List<GioHang> gioHang = gioHangRepository.findByUser(user);
			if (!gioHang.isEmpty()) {
				Set<ChiTietGioHang> chiTietGioHang = gioHang.get(0).getChiTietGioHang();
				Integer giohangId = gioHang.get(0).getId();
				model.addAttribute("chiTietGioHang", chiTietGioHang);
				model.addAttribute("giohangId", giohangId);
				Float totalPriceFloat = gioHang.get(0).getTongtien();
				BigDecimal totalPrice = new BigDecimal(totalPriceFloat).setScale(2, RoundingMode.HALF_UP);
				if (totalPrice.stripTrailingZeros().scale() <= 0) {
					totalPrice = totalPrice.setScale(0, RoundingMode.DOWN);
				}
				model.addAttribute("totalPrice", totalPrice);
			}	
		}
		
		List<Map<String, Object>> productList = productPage.getContent().stream().map(product -> {
			Map<String, Object> data = new HashMap<>();

			BigDecimal gia = new BigDecimal(product.getPrice()).setScale(2, RoundingMode.HALF_UP);
			String giaFormatted = (gia.stripTrailingZeros().scale() <= 0) ? gia.toBigInteger().toString()
					: gia.toString();

			data.put("item", product);
			data.put("gia", giaFormatted);
			return data;
		}).collect(Collectors.toList());
		model.addAttribute("productList", productList);
		model.addAttribute("listBrands", listBrands);
		model.addAttribute("productPage", productPage);
		return "user/shop";
	}

	@GetMapping("/shop/{id}")
	public String item(Model model, @PathVariable Integer id, HttpSession session) {
		Product product = this.productServiceImple.findById(id);
		BigDecimal gia1 = new BigDecimal(product.getPrice()).setScale(2, RoundingMode.HALF_UP);
		String giaFormatted1 = (gia1.stripTrailingZeros().scale() <= 0) ? gia1.toBigInteger().toString()
				: gia1.toString();
		List<Product> topProducts = productServiceImple.getTop4Products();
		List<HinhAnhSanPham> images = product.getHinhAnhSanPhams().stream().filter(image -> !image.isMain()) // Lọc hình
																												// phụ
				.collect(Collectors.toList());
		GioHang gioHang = new GioHang();
		Integer userId = (Integer) session.getAttribute("userId");
		model.addAttribute("userId", userId);
		if(userId != null) {
			User user = userService.findById(userId).get();
			List<GioHang> gioHangs = gioHangRepository.findByUser(user);
			if (!gioHangs.isEmpty()) {
				Set<ChiTietGioHang> chiTietGioHang = gioHangs.get(0).getChiTietGioHang();
				Integer giohangId = gioHangs.get(0).getId();
				model.addAttribute("chiTietGioHang", chiTietGioHang);
				model.addAttribute("giohangId", giohangId);
				Float totalPriceFloat = gioHangs.get(0).getTongtien();
				BigDecimal totalPrice = new BigDecimal(totalPriceFloat).setScale(2, RoundingMode.HALF_UP);
				if (totalPrice.stripTrailingZeros().scale() <= 0) {
					totalPrice = totalPrice.setScale(0, RoundingMode.DOWN);
				}
				model.addAttribute("totalPrice", totalPrice);
			}	
		}
		
		List<Map<String, Object>> productList = topProducts.stream().map(product1 -> {
			Map<String, Object> data = new HashMap<>();

			BigDecimal gia = new BigDecimal(product1.getPrice()).setScale(2, RoundingMode.HALF_UP);
			String giaFormatted = (gia.stripTrailingZeros().scale() <= 0) ? gia.toBigInteger().toString()
					: gia.toString();

			data.put("item", product1);
			data.put("gia", giaFormatted);
			return data;
		}).collect(Collectors.toList());
		model.addAttribute("productList", productList);
		model.addAttribute("giohang", gioHang);
		model.addAttribute("product", product);
		model.addAttribute("topProducts", topProducts);
		model.addAttribute("brand", product.getBrand());
		model.addAttribute("tonkho", product.getSanPhamTonKhos());
		model.addAttribute("image", images);
		model.addAttribute("productId", id);
		model.addAttribute("gia", giaFormatted1);
		return "user/item";
	}
	@GetMapping("/contact")
	public String contact() {
		return "user/contact";
	}
}
