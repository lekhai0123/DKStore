package com.dkstore.controllers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dkstore.models.ChiTietGioHang;
import com.dkstore.models.ChiTietHoaDon;
import com.dkstore.models.GioHang;
import com.dkstore.models.HoaDon;
import com.dkstore.models.PhuongThucThanhToan;
import com.dkstore.models.Product;
import com.dkstore.models.ThanhToan;
import com.dkstore.models.User;
import com.dkstore.repository.ChiTietGioHangRepository;
import com.dkstore.repository.GioHangRepository;
import com.dkstore.repository.HoaDonRepository;
import com.dkstore.repository.ThanhToanRepository;
import com.dkstore.services.ChiTietGioHangService;
import com.dkstore.services.GioHangService;
import com.dkstore.services.ProductService;
import com.dkstore.services.SanPhamTonKhoService;
import com.dkstore.services.ThanhToanService;
import com.dkstore.services.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserControllerMain {
	@Autowired
	private UserService userService;
	@Autowired
	private GioHangService gioHangService;
	@Autowired
	private ProductService productService;
	@Autowired
	private SanPhamTonKhoService sanPhamTonKhoService;
	@Autowired
	private ChiTietGioHangRepository chiTietGioHangRepository;
	@Autowired
	private ThanhToanService thanhToanService;
	@Autowired
	private GioHangRepository gioHangRepository;
	@Autowired
	private ChiTietGioHangService chiTietGioHangService;
	@Autowired
	private ThanhToanRepository thanhToanRepository;
	@Autowired
	private HoaDonRepository hoaDonRepository;

	@PostMapping("/add-giohang")
	@ResponseBody
	public String addGioHang(@RequestParam Integer userId, @RequestParam Integer size, @RequestParam Float gia,
			@RequestParam Integer soluong, @RequestParam Integer productId) {
		Optional<User> user = userService.findById(userId);
		if (user.isPresent()) {
			User userEntity = user.get();
			GioHang gioHang = userEntity.getGioHangs();
			try {
				int result = sanPhamTonKhoService.updateTonKho(size, productId, 0, soluong);
				switch (result) {
				case 1:
					return "Số lượng tồn kho không đủ!";
				case 2:
					return "Không tìm thấy sản phẩm tồn kho!";
				}
				Product product = productService.findById(productId);
				if (gioHang == null) {
					gioHang = new GioHang();
					Float tongtien = gia * soluong;

					gioHang.addDetail(gia, size, soluong, product, tongtien);
					gioHang.setTongtien(tongtien);
					gioHang.setUser(userEntity);

					ThanhToan thanhToan = new ThanhToan();
					thanhToan.setGiohang(gioHang);
					thanhToan.setTrangthai(false);
					gioHang.setThanhtoan(thanhToan);

					gioHangService.create(gioHang);
					thanhToanService.create(thanhToan);
					chiTietGioHangRepository.saveAll(gioHang.getChiTietGioHang());
				} else {
					Float tonggiasanpham = gia * soluong;
					gioHang.addDetail(gia, size, soluong, product, tonggiasanpham);

					Float tongtien = (float) gioHang.getChiTietGioHang().stream()
							.mapToDouble(ChiTietGioHang::getTonggiasanpham).sum();
					gioHang.setTongtien(tongtien);

					gioHangService.create(gioHang);
					chiTietGioHangRepository.saveAll(gioHang.getChiTietGioHang());
				}

				return "success";
			} catch (Exception e) {
				return "Có lỗi xảy ra khi thêm vào giỏ hàng!";
			}
		} else {
			return "Không tìm thấy người dùng!";
		}
	}

	@GetMapping("/giohang")
	public String cart(Model model, HttpSession session) {
		Integer userId = (Integer) session.getAttribute("userId");
		if (userId != null) {

			User user = userService.findById(userId).get();
			List<GioHang> gioHang = gioHangRepository.findByUser(user);
			if (!gioHang.isEmpty()) {
				Set<ChiTietGioHang> chiTietGioHang = gioHang.get(0).getChiTietGioHang();

				List<Map<String, Object>> chitietList = chiTietGioHang.stream().map(chitietgiohang -> {
					Map<String, Object> data = new HashMap<>();

					BigDecimal gia = new BigDecimal(chitietgiohang.getGia()).setScale(2, RoundingMode.HALF_UP);
					String giaFormatted = (gia.stripTrailingZeros().scale() <= 0) ? gia.toBigInteger().toString()
							: gia.toString();

					BigDecimal tongGiaSanPham = new BigDecimal(chitietgiohang.getTonggiasanpham()).setScale(2,
							RoundingMode.HALF_UP);
					String tongGiaSanPhamFormatted = (tongGiaSanPham.stripTrailingZeros().scale() <= 0)
							? tongGiaSanPham.toBigInteger().toString()
							: tongGiaSanPham.toString();

					data.put("chitietgiohang", chitietgiohang);
					data.put("gia", giaFormatted);
					data.put("tonggiasanpham", tongGiaSanPhamFormatted);

					return data;
				}).collect(Collectors.toList());

				// Truyền vào Model
				model.addAttribute("chitietList", chitietList);

				Integer giohangId = gioHang.get(0).getId();
				model.addAttribute("chiTietGioHang", chiTietGioHang);
				model.addAttribute("giohangId", giohangId);
			}
		}
		return "user/cart";
	}

	@GetMapping("/delete-chitietgiohang/{id}")
	public String deletedetail(@PathVariable Integer id, HttpServletRequest request) {
		ChiTietGioHang chiTietGioHang = chiTietGioHangService.findById(id);
		Integer oldSoLuong = chiTietGioHang.getSoLuong();
		Integer product = chiTietGioHang.getProduct().getId();
		GioHang gioHang = chiTietGioHang.getGiohang();
		this.sanPhamTonKhoService.updateTonKho(chiTietGioHang.getSize(), product, oldSoLuong, 0);
		if (this.chiTietGioHangService.delete(id)) {
			Float totalAmount = gioHang.calculateTotal();
			gioHang.setTongtien((float) totalAmount);
			this.gioHangService.update(gioHang);
			String referer = request.getHeader("Referer");
			return "redirect:" + referer;
		} else {
			return "redirect:/user/giohang";
		}
	}

	@GetMapping("/checkout/{id}")
	public String checkout(Model model, HttpSession session, @PathVariable Integer id,
			@RequestParam(required = false, defaultValue = "COD") String paymentMethod,
			@RequestParam(required = false, defaultValue = "/fe/images/bank.png") String qrCodeUrl) {
		model.addAttribute("qrCodeUrl", "/fe/images/bank.png");
		model.addAttribute("paymentMethod", paymentMethod);
		ThanhToan thanhToan = this.thanhToanRepository.findByGiohangId(id);
		if (thanhToan == null) {
			thanhToan = new ThanhToan();
		}
		model.addAttribute("thanhtoan", thanhToan);
		model.addAttribute("idGiohang", id);
		model.addAttribute("phuongthuc", PhuongThucThanhToan.values());
		Integer userId = (Integer) session.getAttribute("userId");
		if (userId != null) {
			User user = userService.findById(userId).get();
			List<GioHang> gioHang = gioHangRepository.findByUser(user);
			if (!gioHang.isEmpty()) {
				Set<ChiTietGioHang> chiTietGioHang = gioHang.get(0).getChiTietGioHang();
				Float totalPriceFloat = gioHang.get(0).getTongtien();
				BigDecimal totalPrice = new BigDecimal(totalPriceFloat).setScale(2, RoundingMode.HALF_UP);
				if (totalPrice.stripTrailingZeros().scale() <= 0) {
					totalPrice = totalPrice.setScale(0, RoundingMode.DOWN);
				}
				Integer giohangId = gioHang.get(0).getId();
				model.addAttribute("chiTietGioHang", chiTietGioHang);
				model.addAttribute("giohangId", giohangId);
				model.addAttribute("totalPrice", totalPrice);
			}
			model.addAttribute("user", user);
			System.out.println(user);
		}
		return "user/checkout";
	}

	@PostMapping("/confirm-thanhtoan")
	public String confirm(@ModelAttribute ThanhToan thanhtoan, @RequestParam Integer idGioHang, HttpSession session,
			Model model) {
		thanhtoan.setGiohang(this.gioHangService.findById(idGioHang));
		thanhtoan.setTrangthai(false);
		if (this.thanhToanService.update(thanhtoan)) {
			PhuongThucThanhToan phuongthuc = thanhtoan.getPhuongthuc();
			model.addAttribute("paymentMethod", phuongthuc.name());
			if (phuongthuc == PhuongThucThanhToan.BANK) {
				return "redirect:/user/checkout/" + idGioHang + "?paymentMethod=" + phuongthuc.name() + "&qrCodeUrl="
						+ "";
			}
		}
		return "user/payment/cod";
	}

	@GetMapping("/hoadon")
	public String hoadon(Model model, HttpSession session) {
		Integer userId = (Integer) session.getAttribute("userId");
		if (userId != null) {
			User user = userService.findById(userId).get();
			List<HoaDon> hoaDons = hoaDonRepository.findByUser(user);
			List<GioHang> gioHang = gioHangRepository.findByUser(user);
			if (!hoaDons.isEmpty() && !gioHang.isEmpty()) {
				Set<ChiTietHoaDon> chiTietHoaDons = hoaDons.get(0).getChiTietHoaDon();
				Float totalPriceFloatHD = hoaDons.get(0).getTongtien();
				BigDecimal totalPriceHD = new BigDecimal(totalPriceFloatHD).setScale(2, RoundingMode.HALF_UP);
				if (totalPriceHD.stripTrailingZeros().scale() <= 0) {
					totalPriceHD = totalPriceHD.setScale(0, RoundingMode.DOWN);
				}
				Integer hoadonId = hoaDons.get(0).getId();
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy h:mm a");
				List<Map<String, Object>> hoaDonWithDetails = hoaDons.stream().map(hoaDon -> {
					Map<String, Object> data = new HashMap<>();
					data.put("hoaDon", hoaDon);
					data.put("formattedNgaylap", hoaDon.getNgaylap().format(formatter));
					data.put("soLuongChiTiet", hoaDon.getChiTietHoaDon().size());
					return data;
				}).collect(Collectors.toList());
				model.addAttribute("hoaDonWithDetail", hoaDonWithDetails);
				model.addAttribute("chiTietHoaDon", chiTietHoaDons);
				model.addAttribute("hoadonId", hoadonId);
				model.addAttribute("totalPrice", totalPriceHD);
				Set<ChiTietGioHang> chiTietGioHang = gioHang.get(0).getChiTietGioHang();
				Float totalPriceFloat = gioHang.get(0).getTongtien();
				BigDecimal totalPrice = new BigDecimal(totalPriceFloat).setScale(2, RoundingMode.HALF_UP);
				if (totalPrice.stripTrailingZeros().scale() <= 0) {
					totalPrice = totalPrice.setScale(0, RoundingMode.DOWN);
				}
				Integer giohangId = gioHang.get(0).getId();
				model.addAttribute("chiTietGioHang", chiTietGioHang);
				model.addAttribute("giohangId", giohangId);
				model.addAttribute("totalPrice", totalPrice);
			}
		}
		return "user/hoadon";
	}

	@GetMapping("/{hoadonId}/chitiet")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> getChiTietHoaDon(@PathVariable Integer hoadonId) {
		try {
			HoaDon hoaDon = hoaDonRepository.findById(hoadonId)
					.orElseThrow(() -> new RuntimeException("HoaDon không tồn tại"));

			Set<ChiTietHoaDon> chiTietHoaDons = hoaDon.getChiTietHoaDon();
			List<Map<String, Object>> chiTietList = new ArrayList<>();
			for (ChiTietHoaDon chiTiet : chiTietHoaDons) {
				Map<String, Object> chiTietData = new HashMap<>();
				chiTietData.put("productName", chiTiet.getProduct().getName());
				chiTietData.put("soluong", chiTiet.getSoluong());
				chiTietData.put("gia", chiTiet.getGia());
				chiTietData.put("tonggiasanpham", chiTiet.getTonggiasanpham());
				chiTietList.add(chiTietData);
			}

			Map<String, Object> response = new HashMap<>();
			response.put("chiTietHoaDons", chiTietList);

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Collections.singletonMap("error", "Có lỗi xảy ra khi xử lý yêu cầu."));
		}
	}

	@PostMapping("/giohang")
	public String updateCartItem(@RequestParam Integer chitietId, @RequestParam Integer newSoluong,
			@RequestParam Integer productId, @RequestParam Integer size, @RequestParam Integer giohangId,
			@RequestParam Float gia) {
		// Giả sử bạn có service để cập nhật giỏ hàng
		ChiTietGioHang chiTietGioHang = chiTietGioHangRepository.findById(chitietId).get();
		Integer soluong = chiTietGioHang.getSoLuong();
		chiTietGioHang.setSoLuong(newSoluong);
		chiTietGioHang.setTonggiasanpham(gia * newSoluong);
		GioHang gioHang = gioHangRepository.findById(giohangId).get();
		Float totalAmount = gioHang.calculateTotal();
		gioHang.setTongtien((float) totalAmount);
		chiTietGioHangService.update(chiTietGioHang);
		sanPhamTonKhoService.updateTonKho(size, productId, soluong, newSoluong);
		// Redirect lại trang giỏ hàng sau khi cập nhật
		return "redirect:/user/giohang"; // Thay đổi URL nếu cần
	}

}
