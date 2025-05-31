package com.dkstore.models;


import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


import jakarta.persistence.*;

@Entity
@Table(name = "hoadon")
public class HoaDon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "ngaylap")
    private LocalDateTime ngaylap;
    
    @PrePersist
    protected void onCreate() {
        ngaylap = LocalDateTime.now(); 
    }

    @Column(name = "tongtien")
    private Float tongtien;

    @Column(name = "trangthai")
    private Boolean trangthai;
    
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @OneToMany(mappedBy = "hoadon", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ChiTietHoaDon> ChiTietHoaDon;

	public HoaDon() {
		this.ChiTietHoaDon = new HashSet<>();
	}

	public HoaDon(Integer id, LocalDateTime ngaylap, Float tongtien, Boolean trangthai, User user,
			Set<com.dkstore.models.ChiTietHoaDon> chiTietHoaDon) {
		super();
		this.id = id;
		this.ngaylap = ngaylap;
		this.tongtien = tongtien;
		this.trangthai = trangthai;
		this.user = user;
		ChiTietHoaDon = chiTietHoaDon;
	}
	

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public LocalDateTime getNgaylap() {
		return ngaylap;
	}

	public void setNgaylap(LocalDateTime ngaylap) {
		this.ngaylap = ngaylap;
	}

	public Float getTongtien() {
		return tongtien;
	}

	public void setTongtien(Float tongtien) {
		this.tongtien = tongtien;
	}

	public Boolean getTrangthai() {
		return trangthai;
	}

	public void setTrangthai(Boolean trangthai) {
		this.trangthai = trangthai;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Set<ChiTietHoaDon> getChiTietHoaDon() {
		return ChiTietHoaDon;
	}

	public void setChiTietHoaDon(Set<ChiTietHoaDon> chiTietHoaDon) {
		ChiTietHoaDon = chiTietHoaDon;
	}

	public void addDetail(Float giaban,Integer size, Integer soluong, Product product, Float tonggiasanpham) {
	    ChiTietHoaDon chiTiet = new ChiTietHoaDon();
	    chiTiet.setGia(giaban);
	    chiTiet.setSize(size);
	    chiTiet.setSoluong(soluong);
	    chiTiet.setProduct(product); // Thiết lập sản phẩm
	    chiTiet.setHoadon(this); // Thiết lập hóa đơn hiện tại cho chi tiết hóa đơn
	    chiTiet.setTonggiasanpham(tonggiasanpham);
	    this.ChiTietHoaDon.add(chiTiet);
	}
	public Float calculateTotal() {
	    Float total = (float) 0;
	    for (ChiTietHoaDon chiTiet : ChiTietHoaDon) {
	        float chiTietTotal = chiTiet.getGia() * chiTiet.getSoluong();	      
	        total += chiTietTotal;
	    }
	    return total;
	}
}
