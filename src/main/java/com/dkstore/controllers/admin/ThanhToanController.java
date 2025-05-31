package com.dkstore.controllers.admin;


import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.dkstore.models.PhuongThucThanhToan;
import com.dkstore.models.ThanhToan;
import com.dkstore.repository.ThanhToanRepository;
import com.dkstore.services.ChiTietHoaDonService;
import com.dkstore.services.GioHangService;
import com.dkstore.services.HoaDonService;
import com.dkstore.services.SanPhamTonKhoService;
import com.dkstore.services.ThanhToanService;


@Controller
@RequestMapping("/admin")
public class ThanhToanController {
	@Autowired
	private ThanhToanService thanhToanService;
	@Autowired
	private GioHangService gioHangService;
	@Autowired
	private ThanhToanRepository thanhToanRepository;
	@Autowired
	private HoaDonService hoaDonService;
	@Autowired
	private ChiTietHoaDonService chiTietHoaDonService;
	@Autowired
	private SanPhamTonKhoService sanPhamTonKhoService;
	@GetMapping("/confirm-thanhtoan/{id}")
	public String index(@PathVariable Integer id,Model model) {
		ThanhToan thanhToan = this.thanhToanRepository.findByGiohangId(id);
		if (thanhToan == null) {
	        thanhToan = new ThanhToan();
	    }
		model.addAttribute("thanhtoan",thanhToan);
		model.addAttribute("idGiohang", id);
		model.addAttribute("phuongthuc", PhuongThucThanhToan.values());
		return "admin/thanhtoan/confirm";
	}
	@PostMapping("/confirm-thanhtoan")
	public String confirm(@ModelAttribute ThanhToan thanhtoan,@RequestParam Integer idGiohang) {
		thanhtoan.setGiohang(this.gioHangService.findById(idGiohang));
		if (this.thanhToanService.update(thanhtoan)) {
			if(thanhtoan.getTrangthai()==true) {
				HoaDon hoaDon = new HoaDon();
				GioHang gioHang = gioHangService.findById(idGiohang);
				hoaDon.setTongtien(gioHang.getTongtien());
				hoaDon.setUser(gioHang.getUser());
				hoaDon.setTrangthai(true);
				hoaDonService.create(hoaDon);
				List<ChiTietGioHang> chiTietGioHangList = new ArrayList<>(gioHang.getChiTietGioHang());
	            for (ChiTietGioHang chiTiet : chiTietGioHangList) {
	                // Tạo chi tiết hóa đơn từ chi tiết giỏ hàng
	                ChiTietHoaDon chiTietHoaDon = new ChiTietHoaDon();
	                chiTietHoaDon.setHoadon(hoaDon); // Gán hóa đơn cho chi tiết hóa đơn
	                chiTietHoaDon.setProduct(chiTiet.getProduct()); // Gán sản phẩm từ chi tiết giỏ hàng
	                chiTietHoaDon.setSoluong(chiTiet.getSoLuong()); // Gán số lượng
	                chiTietHoaDon.setGia(chiTiet.getGia()); // Gán giá
	                chiTietHoaDon.setTonggiasanpham(chiTiet.getTonggiasanpham());

	                // Lưu chi tiết hóa đơn vào cơ sở dữ liệu
	                chiTietHoaDonService.create(chiTietHoaDon);
	            }
	            gioHangService.delete(idGiohang);
			}			
			return "redirect:/admin/giohang";
		} else {
			return "redirect:/admin/confirm-thanhtoan/" + thanhtoan.getGiohang().getId();
		}
	}
}
