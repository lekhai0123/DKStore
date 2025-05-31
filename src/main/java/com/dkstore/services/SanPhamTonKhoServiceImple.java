package com.dkstore.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.dkstore.models.ChiTietGioHang;
import com.dkstore.models.ChiTietHoaDon;
import com.dkstore.models.GioHang;
import com.dkstore.models.Product;
import com.dkstore.models.SanPhamTonKho;
import com.dkstore.repository.ChiTietGioHangRepository;
import com.dkstore.repository.GioHangRepository;
import com.dkstore.repository.SanPhamTonKhoRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class SanPhamTonKhoServiceImple implements SanPhamTonKhoService{
	@Autowired
	private SanPhamTonKhoRepository sanPhamTonKhoRepository;
	@Autowired
	private ChiTietGioHangRepository chiTietGioHangRepository;
	@Autowired
	private GioHangRepository gioHangRepository;
	
	@Override
	public List<SanPhamTonKho> getAll() {
		// TODO Auto-generated method stub
		return this.sanPhamTonKhoRepository.findAll();
	}

	@Override
	public Boolean create(SanPhamTonKho sanPhamTonKho) {
		// TODO Auto-generated method stub
		try {
			this.sanPhamTonKhoRepository.save(sanPhamTonKho);
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public SanPhamTonKho findById(Integer productId) {
		// TODO Auto-generated method stub
		return this.sanPhamTonKhoRepository.findById(productId).get();
	}

	@Override
	public Boolean update(SanPhamTonKho sanPhamTonKho) {
		// TODO Auto-generated method stub
		try {
		    SanPhamTonKho existingDetail = sanPhamTonKhoRepository.findById(sanPhamTonKho.getId()).orElse(null);
		    if (existingDetail != null) {
		        existingDetail.setProduct(sanPhamTonKho.getProduct());
		        existingDetail.setSize(sanPhamTonKho.getSize());
		        existingDetail.setTonkho(sanPhamTonKho.getTonkho());
		        sanPhamTonKhoRepository.save(existingDetail); // Lưu bản ghi đã cập nhật
		        return true;
		    }
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
			this.sanPhamTonKhoRepository.delete(findById(id));
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public Page<SanPhamTonKho> getAll(Integer pageNo) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	@Transactional
	public Integer updateTonKho(Integer size, Integer productId, Integer oldSoLuong, Integer newSoLuong) {
	    // Bước 1: Tính chênh lệch giữa số lượng cũ và mới
	    int chenhLechSoLuong = newSoLuong - (oldSoLuong == null ? 0 : oldSoLuong);

	    // Bước 2: Lấy thông tin từ SanPhamTonKho
	    Optional<SanPhamTonKho> optionalTonKho = sanPhamTonKhoRepository.findBySizeAndProduct(size, productId);
	    if (optionalTonKho.isPresent()) {
	        SanPhamTonKho tonKho = optionalTonKho.get();

	        // Bước 3: Cập nhật tồn kho
	        Integer newTonKho = tonKho.getTonkho() - chenhLechSoLuong;
	        if (newTonKho < 0) {
	            return 1; // Số lượng tồn kho không đủ
	        }
	        tonKho.setTonkho(newTonKho);

	        // Lưu thay đổi
	        sanPhamTonKhoRepository.save(tonKho);
	        return 0; // Thành công
	    } else {
	        return 2; // Không tìm thấy sản phẩm tồn kho
	    }
	}


	@Override
	public Boolean deleteGioHangAndUpdateTonKho(Integer gioHangId) {
	    // Tìm giỏ hàng bằng ID
	    GioHang gioHang = gioHangRepository.findById(gioHangId).orElse(null);

	    if (gioHang != null) {
	        // Lấy danh sách các ChiTietGioHang từ giỏ hàng
	        Set<ChiTietGioHang> chiTietGioHangSet = gioHang.getChiTietGioHang();

	        // Duyệt qua các chi tiết giỏ hàng để lấy thông tin sản phẩm và số lượng
	        for (ChiTietGioHang chiTietGioHang : chiTietGioHangSet) {
	            Integer productId = chiTietGioHang.getProduct().getId();
	            Integer size = chiTietGioHang.getSize();
	            Integer soLuong = chiTietGioHang.getSoLuong();
	            // Tìm sản phẩm tồn kho theo size và productId
	            Optional<SanPhamTonKho> sanPhamTonKho = sanPhamTonKhoRepository.findBySizeAndProduct(size, productId);

	            if (sanPhamTonKho.isPresent()) {
	                // Lấy SanPhamTonKho và cập nhật số lượng tồn kho
	                SanPhamTonKho tonKho = sanPhamTonKho.get();
	                Integer newTonKho = tonKho.getTonkho() + soLuong;  // Tăng số lượng tồn kho
	                tonKho.setTonkho(newTonKho);

	                // Lưu lại sự thay đổi trong tồn kho
	                sanPhamTonKhoRepository.save(tonKho);
	            } else {
	                return false; // Trả về false nếu không tìm thấy sản phẩm tồn kho
	            }

	            // Xóa chi tiết giỏ hàng
	            chiTietGioHangRepository.delete(chiTietGioHang);
	        }

	        // Sau khi xóa tất cả chi tiết giỏ hàng, xóa giỏ hàng
	        gioHangRepository.delete(gioHang);
	        return true; // Trả về true nếu tất cả thao tác thành công
	    } else {
	        return false; // Trả về false nếu không tìm thấy giỏ hàng
	    }
	}

}
