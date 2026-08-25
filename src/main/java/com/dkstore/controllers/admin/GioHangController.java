package com.dkstore.controllers.admin;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.dkstore.models.ChiTietGioHang;
import com.dkstore.models.ChiTietHoaDon;
import com.dkstore.models.GioHang;
import com.dkstore.models.HoaDon;
import com.dkstore.models.Product;
import com.dkstore.models.SanPhamTonKho;
import com.dkstore.models.ThanhToan;
import com.dkstore.models.User;
import com.dkstore.repository.GioHangRepository;
import com.dkstore.repository.SanPhamTonKhoRepository;
import com.dkstore.services.ChiTietGioHangService;
import com.dkstore.services.ChiTietHoaDonService;
import com.dkstore.services.GioHangService;
import com.dkstore.services.HoaDonService;
import com.dkstore.services.ProductService;
import com.dkstore.services.SanPhamTonKhoService;
import com.dkstore.services.ThanhToanService;
import com.dkstore.services.UserService;

import jakarta.servlet.http.HttpServletRequest;


@Controller
@RequestMapping("/admin")
public class GioHangController {
	@Autowired
	private GioHangService gioHangService;
	@Autowired
	private UserService userService;
	@Autowired
	private ProductService productService;
	@Autowired
	private ChiTietGioHangService chiTietGioHangService;
	@Autowired
	private ThanhToanService thanhToanService;
	@Autowired
	private SanPhamTonKhoService sanPhamTonKhoService;
	@Autowired
	private SanPhamTonKhoRepository sanPhamTonKhoRepository;
	@GetMapping("/giohang")
	public String index(Model model, @RequestParam(defaultValue = "1") Integer pageNo,@Param("keyword") String keyword) {
		Page<GioHang> listGiohang = this.gioHangService.getAll(pageNo);
		if(keyword != null) {
			listGiohang = this.gioHangService.search(keyword,pageNo);
			model.addAttribute("keyword",keyword);
		}
		model.addAttribute("giohangs", listGiohang);
		model.addAttribute("totalPage", listGiohang.getTotalPages());
		model.addAttribute("currentPage", pageNo);
		return "admin/giohang/index";
	}
	@GetMapping("/{productId}/size")
	public ResponseEntity<List<SanPhamTonKho>> getSizesByProductId(@PathVariable Integer productId) {
	    Product product = productService.findById(productId);
	    if (product != null) {
	        // Lấy danh sách các kích thước từ sản phẩm
	        List<SanPhamTonKho> sizes = new ArrayList<>(product.getSanPhamTonKhos());
	        
	        // Lọc các bản ghi có tồn kho = 0
	        sizes = sizes.stream()
	                     .filter(size -> size.getTonkho() > 0)  // Giữ lại các kích thước có tồn kho > 0
	                     .collect(Collectors.toList());
	        
	        // Trả về danh sách đã lọc
	        return ResponseEntity.ok(sizes);
	    }
	    return ResponseEntity.notFound().build();
	}
	@GetMapping("/add-giohang")
	public String add(Model model) {
		GioHang giohang= new GioHang();
		model.addAttribute("giohang", giohang);
		List<User> listUsers = this.userService.getAll();
		model.addAttribute("listUsers", listUsers);
		List<Product> listProducts = this.productService.getAll(); 
		model.addAttribute("listProducts", listProducts);
		return "admin/giohang/add";
	}
	@PostMapping("/add-giohang")
	public String save(HttpServletRequest request, @ModelAttribute GioHang giohang) {
	    String giabanList = request.getParameter("giabanList");
	    String sizeList = request.getParameter("sizeList");
	    String soluongList = request.getParameter("soluongList");
	    String productIdList = request.getParameter("productIdList"); 
	    String tongGiaList = request.getParameter("tongGiaList");

	    int tongGia = 0;

	    if (giabanList != null && sizeList != null && soluongList != null && productIdList != null && tongGiaList != null) {
	        String[] giabanStrings = giabanList.split(",");
	        String[] sizeStrings = sizeList.split(",");
	        String[] soluongStrings = soluongList.split(",");
	        String[] productIdStrings = productIdList.split(","); 
	        String[] tongGiaLists = tongGiaList.split(","); 

	        for (int i = 0; i < giabanStrings.length; i++) {
	            if (!giabanStrings[i].isEmpty() && !sizeStrings[i].isEmpty() && !soluongStrings[i].isEmpty() && !productIdStrings[i].isEmpty() && !tongGiaLists[i].isEmpty()) {
	                try {
	                    Float giaban = Float.valueOf(giabanStrings[i]);
	                    Integer size = Integer.valueOf(sizeStrings[i]);
	                    Integer soluong = Integer.valueOf(soluongStrings[i]);
	                    Integer productId = Integer.valueOf(productIdStrings[i]);
	                    Product product = productService.findById(productId);
	                    Float tonggiasanpham = Float.valueOf(tongGiaLists[i]);

	                    giohang.addDetail(giaban,size, soluong, product,tonggiasanpham);
	                    sanPhamTonKhoService.updateTonKho(size, productId,0,soluong);
	                    tongGia += giaban.floatValue() * soluong.floatValue(); 
	                } catch (NumberFormatException e) {
	                    return "admin/giohang/add"; 
	                } catch (IllegalArgumentException e) {
	                    return "admin/giohang/add"; 
	                }
	            }
	        }
	    }
	    giohang.setTongtien((float) tongGia);
	    ThanhToan thanhToan = new ThanhToan();
	    thanhToan.setGiohang(giohang);
	    thanhToan.setTrangthai(false);
	   
	    giohang.setThanhtoan(thanhToan);
	    if (this.gioHangService.create(giohang)) {
		    thanhToanService.create(thanhToan);
		    List<ChiTietGioHang> copyOfChiTietGioHang = new ArrayList<>(giohang.getChiTietGioHang());
	        for (ChiTietGioHang chiTiet : copyOfChiTietGioHang) {
	            chiTietGioHangService.create(chiTiet);
	        }
	        return "redirect:/admin/giohang"; 
	    } else {
	        return "admin/giohang/add"; 
	    }
	}
	@GetMapping("/detail-giohang/{id}")
	public String getHoaDonDetails(Model model,@PathVariable Integer id) {
		GioHang gioHang = this.gioHangService.findById(id);
		model.addAttribute("giohang",gioHang);
		List<User> listUsers = this.userService.getAll();
		model.addAttribute("listUsers", listUsers);
		List<Product> listProducts = this.productService.getAll(); 
		model.addAttribute("listProducts", listProducts);
		Float totalAmount = gioHang.calculateTotal();
        gioHang.setTongtien(totalAmount); 
        this.gioHangService.update(gioHang);
	    return "admin/giohang/detail"; // Giả sử bạn có phương thức để lấy hóa đơn theo ID
	}
	@GetMapping("/edit-giohang/{id}")
	public String edit(Model model, @PathVariable Integer id) {
		GioHang gioHang = this.gioHangService.findById(id);
		model.addAttribute("giohang",gioHang);
		List<User> listUsers = this.userService.getAll();
		model.addAttribute("listUsers", listUsers);
		List<Product> listProducts = this.productService.getAll(); 
		model.addAttribute("listProducts", listProducts);
		model.addAttribute("chiTietHoaDons", gioHang.getChiTietGioHang()); 
		return "admin/giohang/edit";
	}
	@PostMapping("/edit-giohang")
	public String update(@ModelAttribute GioHang giohang) {
		LocalDateTime now = LocalDateTime.now();
		giohang.setThoigiantao(now);
		this.gioHangService.update(giohang);
		return "redirect:/admin/giohang";		
	}
	@GetMapping("/add-chitietgiohang")
	public String adddetail(@RequestParam Integer idGiohang,Model model) {
		ChiTietGioHang chiTietGioHang = new ChiTietGioHang();
		model.addAttribute("chitiet",chiTietGioHang);
		List<Product> listProducts = this.productService.getAll(); 
		model.addAttribute("listProducts", listProducts);
		model.addAttribute("idgiohang",idGiohang);
		return "admin/giohang/chitiet/add";
	}
	@PostMapping("/add-chitietgiohang")
	public String savedetail(@RequestParam Integer idgiohang, @RequestParam Integer product, @ModelAttribute ChiTietGioHang chiTietGioHang) {
	    GioHang gioHang = gioHangService.findById(idgiohang);
	    if (gioHang != null) {
	        chiTietGioHang.setGiohang(gioHang);
	    }	
	    Product product1 = productService.findById(product);
	    if (product != null) {
	        chiTietGioHang.setProduct(product1); 
	    }
	    boolean isCreated = this.chiTietGioHangService.create(chiTietGioHang);
	    if (isCreated) {
	        gioHang = gioHangService.findById(gioHang.getId());  
	        Float totalAmount = gioHang.calculateTotal();
	        gioHang.setTongtien(totalAmount); 
	        this.gioHangService.update(gioHang);
	        this.sanPhamTonKhoService.updateTonKho(chiTietGioHang.getSize(), product,0,chiTietGioHang.getSoLuong());
	        return "redirect:/admin/detail-giohang/" + chiTietGioHang.getGiohang().getId();
	    } else {
	        return "admin/giohang/chitiet/add";
	    }
	}


	@GetMapping("/edit-chitietgiohang/{id}")
	public String editdetail(Model model, @PathVariable Integer id) {
	    ChiTietGioHang chiTietGioHang = this.chiTietGioHangService.findById(id);
	    model.addAttribute("chitiet", chiTietGioHang);
	    model.addAttribute("tongia", chiTietGioHang.getTonggiasanpham());
	    GioHang gioHang = chiTietGioHang.getGiohang();
	    model.addAttribute("giohang", gioHang);
	    List<Product> productList = this.productService.getAll();
	    model.addAttribute("product", productList);
	    return "admin/giohang/chitiet/edit";
	}
	@PostMapping("/edit-chitietgiohang")
	public String updatedetail(@ModelAttribute ChiTietGioHang chiTietGioHang) {
	    GioHang gioHang = this.gioHangService.findById(chiTietGioHang.getGiohang().getId());
	    Integer product = chiTietGioHang.getProduct().getId();
	    if (gioHang != null) {
	        ChiTietGioHang oldDetail = this.chiTietGioHangService.findById(chiTietGioHang.getId());
	        Integer oldSoLuong = oldDetail != null ? oldDetail.getSoLuong() : null;
	        if (this.chiTietGioHangService.update(chiTietGioHang)) {
	            Float totalAmount = gioHang.calculateTotal();
	            gioHang.setTongtien((float) totalAmount);
	            this.gioHangService.update(gioHang); 
	            this.sanPhamTonKhoService.updateTonKho(
	                chiTietGioHang.getSize(), 
	                product, 
	                oldSoLuong, 
	                chiTietGioHang.getSoLuong()
	            );
	            return "redirect:/admin/detail-giohang/" + chiTietGioHang.getGiohang().getId();
	        } else {
	            return "admin/giohang/chitiet/edit";
	        }
	    } else {
	        return "admin/giohang/chitiet/edit";
	    }
	}
	@GetMapping("/delete-giohang/{id}")
	public String delete(@PathVariable Integer id) {
		if (sanPhamTonKhoService.deleteGioHangAndUpdateTonKho(id)) {
			return "redirect:/admin/giohang";
		} else {
			return "redirect:/admin/giohang";
		}
	}
	@GetMapping("/delete-chitietgiohang/{id}")
	public String deletedetail(@PathVariable Integer id) {
		ChiTietGioHang chiTietGioHang= chiTietGioHangService.findById(id);
		Integer oldSoLuong = chiTietGioHang.getSoLuong();
		Integer product = chiTietGioHang.getProduct().getId();
		GioHang gioHang= chiTietGioHang.getGiohang();
		this.sanPhamTonKhoService.updateTonKho(
				chiTietGioHang.getSize(), 
				product, 
				oldSoLuong, 
				0
				);
		if (this.chiTietGioHangService.delete(id)) {
            Float totalAmount = gioHang.calculateTotal();
            gioHang.setTongtien((float)totalAmount);
            this.gioHangService.update(gioHang); 
            return "redirect:/admin/detail-giohang/" + chiTietGioHang.getGiohang().getId();
		} else {
			return "redirect:/admin/detail-giohang/" + chiTietGioHang.getGiohang().getId();
		}
	}
}
