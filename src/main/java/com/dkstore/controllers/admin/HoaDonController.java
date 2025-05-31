package com.dkstore.controllers.admin;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

import com.dkstore.models.ChiTietGioHang;
import com.dkstore.models.ChiTietHoaDon;
import com.dkstore.models.HoaDon;
import com.dkstore.models.Product;
import com.dkstore.models.User;
import com.dkstore.services.ChiTietHoaDonService;
import com.dkstore.services.HoaDonService;
import com.dkstore.services.ProductService;
import com.dkstore.services.SanPhamTonKhoService;
import com.dkstore.services.UserService;

import jakarta.servlet.http.HttpServletRequest;


@Controller
@RequestMapping("/admin")
public class HoaDonController {
	@Autowired
	private UserService userService;
	@Autowired
	private HoaDonService hoaDonService;
	@Autowired
	private ProductService productService;
	@Autowired
	private ChiTietHoaDonService chiTietHoaDonService;
	@Autowired
	private SanPhamTonKhoService sanPhamTonKhoService;
	@GetMapping("/hoadon")
	public String index(Model model, @RequestParam(defaultValue = "1") Integer pageNo,@Param("keyword") String keyword) {
		Page<HoaDon> listHoadons= this.hoaDonService.getAll(pageNo);
		if(keyword != null) {
			listHoadons = this.hoaDonService.search(keyword,pageNo);
			model.addAttribute("keyword",keyword);
		}
		model.addAttribute("hoadon", listHoadons);
		model.addAttribute("totalPage", listHoadons.getTotalPages());
		model.addAttribute("currentPage", pageNo);
		return "/admin/hoadon/index";
	}
	@GetMapping("/add-hoadon")
	public String add(Model model) {
		HoaDon hoadon= new HoaDon();
		model.addAttribute("hoadon", hoadon);
		List<User> listUsers = this.userService.getAll();
		model.addAttribute("listUser", listUsers);
		List<Product> listProducts = this.productService.getAll(); 
		model.addAttribute("listProducts", listProducts);
		return "/admin/hoadon/add";
	}
	@PostMapping("/add-hoadon")
	public String save(HttpServletRequest request, @ModelAttribute HoaDon hoaDon) {
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

	                    hoaDon.addDetail(giaban,size, soluong, product,tonggiasanpham);
	                    sanPhamTonKhoService.updateTonKho(size, productId,0,soluong);
	                    tongGia += giaban.floatValue() * soluong.floatValue(); 
	                } catch (NumberFormatException e) {
	                    return "admin/hoadon/add"; 
	                } catch (IllegalArgumentException e) {
	                    return "admin/hoadon/add"; 
	                }
	            }
	        }
	    }
	    hoaDon.setTongtien((float) tongGia);
	    hoaDon.setTrangthai(true);
	    if (this.hoaDonService.create(hoaDon)) {
	    	List<ChiTietHoaDon> copyOfChiTietHoaDons = new ArrayList<>(hoaDon.getChiTietHoaDon());
	        for (ChiTietHoaDon chiTiet : copyOfChiTietHoaDons) {
	            chiTietHoaDonService.create(chiTiet);
	        }
	        return "redirect:/admin/hoadon"; 
	    } else {
	        return "admin/hoadon/add"; 
	    }
	}
	@GetMapping("/detail-hoadon/{id}")
	public String getHoaDonDetails(Model model,@PathVariable Integer id) {
		HoaDon hoaDon = this.hoaDonService.findById(id);
		model.addAttribute("hoadon",hoaDon);
		List<User> listUsers = this.userService.getAll();
		model.addAttribute("listUser", listUsers);
		List<Product> listProducts = this.productService.getAll(); 
		model.addAttribute("listProducts", listProducts);
		Float totalAmount = hoaDon.calculateTotal();
        hoaDon.setTongtien((float)totalAmount);
        this.hoaDonService.update(hoaDon); 	
	    return "admin/hoadon/detail"; // Giả sử bạn có phương thức để lấy hóa đơn theo ID
	}
	@GetMapping("/edit-hoadon/{id}")
	public String edit(Model model, @PathVariable Integer id) {
		HoaDon hoaDon = this.hoaDonService.findById(id);
		model.addAttribute("hoadon",hoaDon);
		List<User> listUsers = this.userService.getAll();
		model.addAttribute("listUser", listUsers);
		List<Product> listProducts = this.productService.getAll(); 
		model.addAttribute("listProducts", listProducts);
		model.addAttribute("chiTietHoaDons", hoaDon.getChiTietHoaDon()); 
		return "admin/hoadon/edit";
	}
	@PostMapping("/edit-hoadon")
	public String update(@ModelAttribute HoaDon hoaDon) {
		LocalDateTime now = LocalDateTime.now();
		hoaDon.setNgaylap(now);
		this.hoaDonService.update(hoaDon);
		return "redirect:/admin/hoadon";
		
	}
	@GetMapping("/add-chitiethoadon")
	public String adddetail(@RequestParam Integer idHoadon,Model model) {
		ChiTietHoaDon chiTietHoaDon = new ChiTietHoaDon();
		model.addAttribute("chitiet",chiTietHoaDon);
		List<Product> listProducts = this.productService.getAll(); 
		model.addAttribute("listProducts", listProducts);
		model.addAttribute("idhoadon",idHoadon);
		return "admin/hoadon/chitiet/add";
	}
	@PostMapping("/add-chitiethoadon")
	public String savedetail(@RequestParam Integer idHoadon,@RequestParam Integer product,@ModelAttribute ChiTietHoaDon chiTietHoaDon) {
	    HoaDon hoaDon = hoaDonService.findById(idHoadon);
	    if (hoaDon != null) {
	        chiTietHoaDon.setHoadon(hoaDon);  // Gán hóa đơn cho chi tiết hóa đơn
	    }
	    Product proDuct = productService.findById(product);
	    if (proDuct != null) {
	        chiTietHoaDon.setProduct(proDuct);
	    }
	    boolean isCreated = this.chiTietHoaDonService.create(chiTietHoaDon);
	    if (isCreated) {
            Float totalAmount = hoaDon.calculateTotal();
            hoaDon.setTongtien((float)totalAmount);
            this.hoaDonService.update(hoaDon); 
	        return "redirect:/admin/detail-hoadon/" + chiTietHoaDon.getHoadon().getId();
	    } else {
	        return "/admin/hoadon/chitiet/add";
	    }
	}
	@GetMapping("/edit-chitiethoadon/{id}")
	public String editdetail(Model model, @PathVariable Integer id) {
	    ChiTietHoaDon chiTietHoaDon = this.chiTietHoaDonService.findById(id);
	    model.addAttribute("chitiet", chiTietHoaDon);
	    model.addAttribute("tonggia",chiTietHoaDon.getTonggiasanpham());
	    System.out.println(chiTietHoaDon.getTonggiasanpham());
	    HoaDon hoaDon = chiTietHoaDon.getHoadon();
	    model.addAttribute("hoadon", hoaDon);
	    List<Product> productList = this.productService.getAll();
	    model.addAttribute("product", productList);

	    return "/admin/hoadon/chitiet/edit";
	}
	@PostMapping("/edit-chitiethoadon")
	public String updatedetail(@ModelAttribute ChiTietHoaDon chiTietHoaDon) {
	    HoaDon hoaDon = this.hoaDonService.findById(chiTietHoaDon.getHoadon().getId());	  
	    if (hoaDon != null) {
	        if (this.chiTietHoaDonService.update(chiTietHoaDon)) {
	            Float totalAmount = hoaDon.calculateTotal();
	            hoaDon.setTongtien((float)totalAmount);
	            this.hoaDonService.update(hoaDon); 
	            return "redirect:/admin/detail-hoadon/" + chiTietHoaDon.getHoadon().getId();
	        } else {
	            return "admin/hoadon/chitiet/edit";
	        }
	    } else {
	        return "admin/hoadon/chitiet/edit";
	    }
	}
	@GetMapping("/delete-hoadon/{id}")
	public String delete(@PathVariable Integer id) {
		if (this.hoaDonService.delete(id)) {
			return "redirect:/admin/hoadon";
		} else {
			return "redirect:/admin/hoadon";
		}
	}
	@GetMapping("/delete-chitiethoadon/{id}")
	public String deletedetail(@PathVariable Integer id) {
		ChiTietHoaDon chiTietHoaDon = chiTietHoaDonService.findById(id);
		HoaDon hoaDon = chiTietHoaDon.getHoadon();
		if (this.chiTietHoaDonService.delete(id)) {
            Float totalAmount = hoaDon.calculateTotal();
            hoaDon.setTongtien((float)totalAmount);
            this.hoaDonService.update(hoaDon); 
            return "redirect:/admin/detail-hoadon/" + chiTietHoaDon.getHoadon().getId();
		} else {
			return "redirect:/admin/detail-hoadon/" + chiTietHoaDon.getHoadon().getId();
		}
	}
}
