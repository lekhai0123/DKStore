package com.dkstore.models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "giohang")
public class GioHang {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tongtien")
    private Float tongtien;
    
    @Column(name = "thoigiantao")
    private LocalDateTime thoigiantao;

    @PrePersist
    protected void onCreate() {
    	thoigiantao = LocalDateTime.now(); // Cập nhật thời gian khi đối tượng được tạo
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @OneToMany(mappedBy = "giohang", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ChiTietGioHang> chiTietGioHang;

    @OneToOne(mappedBy = "giohang", cascade = CascadeType.ALL)
    private ThanhToan thanhtoan;

	public GioHang() {
		this.chiTietGioHang = new HashSet<>();
	}

	public GioHang(Integer id, Float tongtien, LocalDateTime thoigiantao, User user, Set<ChiTietGioHang> chiTietGioHang,
			ThanhToan thanhtoan) {
		super();
		this.id = id;
		this.tongtien = tongtien;
		this.thoigiantao = thoigiantao;
		this.user = user;
		this.chiTietGioHang = chiTietGioHang;
		this.thanhtoan = thanhtoan;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Float getTongtien() {
		return tongtien;
	}

	public void setTongtien(Float tongtien) {
		this.tongtien = tongtien;
	}

	public LocalDateTime getThoigiantao() {
		return thoigiantao;
	}

	public void setThoigiantao(LocalDateTime thoigiantao) {
		this.thoigiantao = thoigiantao;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Set<ChiTietGioHang> getChiTietGioHang() {
		return chiTietGioHang;
	}

	public void setChiTietGioHang(Set<ChiTietGioHang> chiTietGioHang) {
		this.chiTietGioHang = chiTietGioHang;
	}

	public ThanhToan getThanhtoan() {
		return thanhtoan;
	}

	public void setThanhtoan(ThanhToan thanhtoan) {
		this.thanhtoan = thanhtoan;
	}
	public void addDetail(Float giaban,Integer size, Integer soluong, Product product, Float tonggiasanpham) {
	    ChiTietGioHang chiTiet = new ChiTietGioHang();
	    chiTiet.setGia(giaban);
	    chiTiet.setSize(size);
	    chiTiet.setSoLuong(soluong);
	    chiTiet.setProduct(product); // Thiết lập sản phẩm
	    chiTiet.setGiohang(this); // Thiết lập hóa đơn hiện tại cho chi tiết hóa đơn
	    chiTiet.setTonggiasanpham(tonggiasanpham);
	    this.chiTietGioHang.add(chiTiet);
	}
	public Float calculateTotal() {
	    Float total = (float) 0;
	    
	    // Duyệt qua từng chi tiết giỏ hàng và tính tổng
	    for (ChiTietGioHang chiTiet : chiTietGioHang) {
	        float chiTietTotal = chiTiet.getGia() * chiTiet.getSoLuong();
	        total += chiTietTotal;
	    }
	    return total;
	}
}
