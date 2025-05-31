package com.dkstore.controllers.admin;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
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
import com.dkstore.models.ChiTietHoaDon;
import com.dkstore.models.HinhAnhSanPham;
import com.dkstore.models.HoaDon;
import com.dkstore.models.Product;
import com.dkstore.models.SanPhamTonKho;
import com.dkstore.models.User;
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
	    model.addAttribute("pageSize", size);  // Thêm tham số size vào model để giữ lại giá trị đã chọn trong select
	    
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
	            hinhAnhSanPham.setMain(true); // Đánh dấu ảnh đầu tiên là ảnh chính
	            isMainImageSet = true; // Đánh dấu đã thiết lập ảnh chính
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
	public String getProductDetails(Model model,@PathVariable Integer id) {
		Product product = this.productService.findById(id);
		model.addAttribute("product",product);
		List<SanPhamTonKho> sanPhamTonKhos= this.sanPhamTonKhoService.getAll(); 
		model.addAttribute("tonkho", sanPhamTonKhos);	
	    return "admin/product/detail"; // Giả sử bạn có phương thức để lấy hóa đơn theo ID
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
	    // Lấy sản phẩm hiện tại từ DB
	    Product existingProduct = this.productService.findById(product.getId());
	    if (existingProduct == null) {
	        System.out.println("Product not found with ID: " + product.getId());
	        // Nếu không tìm thấy sản phẩm, xử lý lỗi
	        return "redirect:/admin/product?error=not-found";
	    }

	    System.out.println("Existing product found: " + existingProduct.getName());

	    // Cập nhật thông tin sản phẩm (trừ hình ảnh)
	    existingProduct.setName(product.getName());
	    existingProduct.setDescription(product.getDescription());
	    existingProduct.setPrice(product.getPrice());
	    existingProduct.setBrand(product.getBrand());

	    System.out.println("Updated product details: ");
	    System.out.println("Name: " + product.getName());
	    System.out.println("Description: " + product.getDescription());
	    System.out.println("Price: " + product.getPrice());
	    System.out.println("Brand: " + product.getBrand());

	    // Lấy các hình ảnh cũ của sản phẩm
	    Set<HinhAnhSanPham> existingImages = existingProduct.getHinhAnhSanPhams();
	    System.out.println("Existing images count: " + (existingImages != null ? existingImages.size() : 0));

	    System.out.println("Number of files uploaded: " + (files != null ? files.length : 0));

	    // Chỉ xóa các hình ảnh cũ nếu có hình ảnh mới được truyền vào
	    if (files != null && files.length > 0) {
	        // Kiểm tra xem mảng files có chứa tệp hợp lệ không
	        boolean hasValidFile = false;
	        for (MultipartFile file : files) {
	            if (!file.isEmpty()) {
	                hasValidFile = true;
	                System.out.println("Valid file found: " + file.getOriginalFilename());
	                break;
	            }
	        }

	        // Nếu có file hợp lệ
	        if (hasValidFile) {
	            // Xóa các hình ảnh cũ
	            if (existingImages != null) {
	                Iterator<HinhAnhSanPham> iterator = existingImages.iterator();
	                while (iterator.hasNext()) {
	                    HinhAnhSanPham image = iterator.next();
	                    try {
	                        Path oldFilePath = Paths.get("src/main/resources/static" + image.getUrlImage());
	                        if (Files.exists(oldFilePath)) {
	                            Files.delete(oldFilePath);
	                            System.out.println("Deleted old image: " + oldFilePath);
	                        }
	                    } catch (IOException e) {
	                        System.out.println("Error deleting image: " + e.getMessage());
	                    }
	                    iterator.remove(); // Xóa khỏi collection
	                }
	            }

	            // Thêm hình ảnh mới
	            List<String> filePaths = this.storageService.storeMultiple(files);
	            System.out.println("Stored file paths: " + filePaths);

	            boolean isMainImageSet = false;
	            for (String filePath : filePaths) {
	                HinhAnhSanPham image = new HinhAnhSanPham();
	                image.setUrlImage(filePath);
	                image.setProduct(existingProduct);
	                if (!isMainImageSet) {
	                    image.setMain(true); // Đánh dấu ảnh đầu tiên là ảnh chính
	                    isMainImageSet = true; // Đánh dấu đã thiết lập ảnh chính
	                    System.out.println("Main image set: " + filePath);
	                }
	                existingImages.add(image); // Thêm vào collection hiện có
	            }
	        } else {
	            System.out.println("No valid files found to upload.");
	        }
	    }

	    // Nếu không có hình ảnh mới thì không xóa hình ảnh cũ
	    System.out.println("Existing images after update: " + existingImages.size());

	    // Lưu sản phẩm
	    if (this.productService.update(existingProduct)) {
	        Integer productId = product.getId();
	        productService.setMainImageForProduct(productId);
	        System.out.println("Product updated successfully, redirecting...");
	        return "redirect:/admin/product?success=updated";
	    }

	    System.out.println("Failed to update product, redirecting...");
	    return "redirect:/admin/product";
	}

	@GetMapping("/add-chitietsanpham")
	public String adddetail(@RequestParam Integer idSanpham,Model model) {
		SanPhamTonKho sanPhamTonKho= new SanPhamTonKho();
		model.addAttribute("tonkho",sanPhamTonKho);
		Product products = productService.findById(idSanpham);
		model.addAttribute("products",products);
		model.addAttribute("idsanpham",idSanpham);
		return "admin/product/chitiet/add";
	}
	@PostMapping("/add-chitietsanpham")
	public String savedetail(@RequestParam Integer idSanpham,@ModelAttribute SanPhamTonKho sanPhamTonKho) {
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
	    Product product= sanPhamTonKho.getProduct();
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
		// Tìm sản phẩm theo ID
		Product existingProduct = this.productService.findById(id);

		if (existingProduct != null) {
			List<HinhAnhSanPham> hinhAnhSanPham = hinhAnhSanPhamService.findByProductId(id);
			// Lấy đường dẫn ảnh của sản phẩm
			for(HinhAnhSanPham image : hinhAnhSanPham) {
				String imagePath = image.getUrlImage();
				Path imageFile = Paths.get("src/main/resources/static" + imagePath);
				try {
					if (Files.exists(imageFile)) {
						Files.delete(imageFile);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (this.productService.delete(id)) {
				return "redirect:/admin/product";
			}
		}

		return "redirect:/admin/product";
	}

	@GetMapping("/delete-chitietsanpham/{id}")
	public String deletedetail(@PathVariable Integer id) {
		SanPhamTonKho sanPhamTonKho = sanPhamTonKhoService.findById(id);
		Product product = sanPhamTonKho.getProduct();
		if (this.sanPhamTonKhoService.delete(id)) { 
            return "redirect:/admin/detail-product/" + sanPhamTonKho.getProduct().getId();
		} else {
			return "redirect:/admin/detail-product/" + sanPhamTonKho.getProduct().getId();
		}
	}
}
