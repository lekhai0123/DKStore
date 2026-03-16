package com.dkstore.controllers.admin;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.dkstore.models.Brand;
import com.dkstore.models.HinhAnhSanPham;
import com.dkstore.models.Product;
import com.dkstore.models.SanPhamTonKho;
import com.dkstore.services.BrandService;
import com.dkstore.services.HinhAnhSanPhamService;
import com.dkstore.services.ProductService;
import com.dkstore.services.SanPhamTonKhoService;
import com.dkstore.services.StorageService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin")
public class ProductController {
	@Autowired
	private BrandService categoryService;
	@Autowired
	private ProductService productService;
	@Autowired
	private StorageService storageService;
	@Autowired
	private SanPhamTonKhoService sanPhamTonKhoService;
	@Autowired
	private HinhAnhSanPhamService hinhAnhSanPhamService;

	@GetMapping("/product")
	public String index(Model model,
			@RequestParam(defaultValue = "1") Integer pageNo,
			@RequestParam(defaultValue = "5") Integer size,
			@RequestParam(required = false) String keyword) {
		Page<Product> listProducts = this.productService.getAll(pageNo, size);

		if (keyword != null) {
			listProducts = this.productService.search(keyword, pageNo, size);
			model.addAttribute("keyword", keyword);
		}
		model.addAttribute("listProducts", listProducts);
		model.addAttribute("totalPage", listProducts.getTotalPages());
		model.addAttribute("currentPage", pageNo);
		model.addAttribute("pageSize", size);

		return "/admin/product/index";
	}

	@GetMapping("/add-product")
	public String add(Model model) {
		Product product = new Product();
		model.addAttribute("product", product);
		List<Brand> listCate = this.categoryService.getAll();
		model.addAttribute("listCate", listCate);
		return "/admin/product/add";
	}

	@PostMapping("/add-product")
	public String save(@ModelAttribute Product product, @RequestParam("images") MultipartFile[] file,
			HttpServletRequest request) {

		List<String> imageUrls = storageService.storeMultiple(file);
		boolean isMainImageSet = false;
		for (String imageUrl : imageUrls) {
			HinhAnhSanPham hinhAnhSanPham = new HinhAnhSanPham();
			hinhAnhSanPham.setUrlImage(imageUrl);
			hinhAnhSanPham.setProduct(product);
			if (!isMainImageSet) {
				hinhAnhSanPham.setMain(true);
				isMainImageSet = true;
			}
			product.getHinhAnhSanPhams().add(hinhAnhSanPham);
		}

		String sizeList = request.getParameter("sizeList");
		String soluongList = request.getParameter("soluongList");
		if (sizeList != null && soluongList != null) {
			String[] sizeStrings = sizeList.split(",");
			String[] soluongStrings = soluongList.split(",");
			for (int i = 0; i < sizeStrings.length; i++) {
				if (!sizeStrings[i].isEmpty() && !soluongStrings[i].isEmpty()) {
					try {
						Integer size = Integer.valueOf(sizeStrings[i]);
						Integer soluong = Integer.valueOf(soluongStrings[i]);
						product.addDetail(size, soluong);
					} catch (NumberFormatException e) {
						return "admin/hoadon/add";
					} catch (IllegalArgumentException e) {
						return "admin/hoadon/add";
					}
				}
			}
		}

		if (this.productService.create(product)) {
			Integer productId = product.getId();
			productService.setMainImageForProduct(productId);
			for (SanPhamTonKho sanPhamTonKho : product.getSanPhamTonKhos()) {
				Product product1 = productService.findById(product.getId());
				sanPhamTonKho.setProduct(product1);
				sanPhamTonKhoService.create(sanPhamTonKho);
			}
			return "redirect:/admin/product";
		}
		return "admin/product/add";
	}

	@GetMapping("/detail-product/{id}")
	public String getProductDetails(Model model, @PathVariable Integer id) {
		Product product = this.productService.findById(id);
		model.addAttribute("product", product);
		List<SanPhamTonKho> sanPhamTonKhos = this.sanPhamTonKhoService.getAll();
		model.addAttribute("tonkho", sanPhamTonKhos);
		return "admin/product/detail";
	}

	@GetMapping("/edit-product/{id}")
	public String edit(Model model, @PathVariable Integer id) {
		Product product = this.productService.findById(id);
		model.addAttribute("product", product);
		List<Brand> listCate = this.categoryService.getAll();
		model.addAttribute("listCate", listCate);
		return "admin/product/edit";
	}

	@PostMapping("/edit-product")
	public String update(@ModelAttribute Product product,
			@RequestParam(value = "image_url1", required = false) MultipartFile[] files) {
		Product existingProduct = this.productService.findById(product.getId());
		if (existingProduct == null) {
			return "redirect:/admin/product?error=not-found";
		}

		existingProduct.setName(product.getName());
		existingProduct.setDescription(product.getDescription());
		existingProduct.setPrice(product.getPrice());
		existingProduct.setBrand(product.getBrand());

		Set<HinhAnhSanPham> existingImages = existingProduct.getHinhAnhSanPhams();

		if (files != null && files.length > 0) {
			boolean hasValidFile = false;
			for (MultipartFile file : files) {
				if (file != null && !file.isEmpty()) {
					hasValidFile = true;
					break;
				}
			}

			if (hasValidFile) {
				if (existingImages != null) {
					Iterator<HinhAnhSanPham> iterator = existingImages.iterator();
					while (iterator.hasNext()) {
						iterator.next();
						iterator.remove();
					}
				}

				List<String> filePaths = this.storageService.storeMultiple(files);
				boolean isMainImageSet = false;
				for (String filePath : filePaths) {
					HinhAnhSanPham image = new HinhAnhSanPham();
					image.setUrlImage(filePath);
					image.setProduct(existingProduct);
					if (!isMainImageSet) {
						image.setMain(true);
						isMainImageSet = true;
					}
					existingImages.add(image);
				}
			}
		}

		if (this.productService.update(existingProduct)) {
			Integer productId = product.getId();
			productService.setMainImageForProduct(productId);
			return "redirect:/admin/product?success=updated";
		}

		return "redirect:/admin/product";
	}

	@GetMapping("/add-chitietsanpham")
	public String adddetail(@RequestParam Integer idSanpham, Model model) {
		SanPhamTonKho sanPhamTonKho = new SanPhamTonKho();
		model.addAttribute("tonkho", sanPhamTonKho);
		Product products = productService.findById(idSanpham);
		model.addAttribute("products", products);
		model.addAttribute("idsanpham", idSanpham);
		return "admin/product/chitiet/add";
	}

	@PostMapping("/add-chitietsanpham")
	public String savedetail(@RequestParam Integer idSanpham, @ModelAttribute SanPhamTonKho sanPhamTonKho) {
		Product proDuct = productService.findById(idSanpham);
		if (proDuct != null) {
			sanPhamTonKho.setProduct(proDuct);
		}
		boolean isCreated = this.sanPhamTonKhoService.create(sanPhamTonKho);
		if (isCreated) {
			return "redirect:/admin/detail-product/" + sanPhamTonKho.getProduct().getId();
		} else {
			return "/admin/product/chitiet/add";
		}
	}

	@GetMapping("/edit-chitietsanpham/{id}")
	public String editdetail(Model model, @PathVariable Integer id) {
		SanPhamTonKho sanPhamTonKho = this.sanPhamTonKhoService.findById(id);
		model.addAttribute("tonkho", sanPhamTonKho);
		Product product = sanPhamTonKho.getProduct();
		model.addAttribute("product", product);
		return "/admin/product/chitiet/edit";
	}

	@PostMapping("/edit-chitietsanpham")
	public String updatedetail(@ModelAttribute SanPhamTonKho sanPhamTonKho) {
		Product product = this.productService.findById(sanPhamTonKho.getProduct().getId());
		if (product != null) {
			if (this.sanPhamTonKhoService.update(sanPhamTonKho)) {
				return "redirect:/admin/detail-product/" + sanPhamTonKho.getProduct().getId();
			} else {
				return "admin/product/chitiet/edit";
			}
		} else {
			return "admin/product/chitiet/edit";
		}
	}

	@GetMapping("/delete-product/{id}")
	public String delete(@PathVariable Integer id) {
		Product existingProduct = this.productService.findById(id);

		if (existingProduct != null) {
			if (this.productService.delete(id)) {
				return "redirect:/admin/product";
			}
		}

		return "redirect:/admin/product";
	}

	@GetMapping("/delete-chitietsanpham/{id}")
	public String deletedetail(@PathVariable Integer id) {
		SanPhamTonKho sanPhamTonKho = sanPhamTonKhoService.findById(id);
		if (this.sanPhamTonKhoService.delete(id)) {
			return "redirect:/admin/detail-product/" + sanPhamTonKho.getProduct().getId();
		} else {
			return "redirect:/admin/detail-product/" + sanPhamTonKho.getProduct().getId();
		}
	}
}