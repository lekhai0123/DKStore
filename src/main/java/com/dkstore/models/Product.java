package com.dkstore.models;


import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {
	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	@Column(name = "name")
	private String name;
	@Column(name = "description", length = 4000)
	private String description;
	@Column(name = "price")
	private Float price;
	@Column(name = "main_image")
	private String mainImage;
	@ManyToOne
	@JoinColumn(name = "brand_id",referencedColumnName = "id")
	private Brand brand;
	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ChiTietGioHang> chiTietGioHangs = new HashSet<>();	
	@OneToMany(mappedBy = "product", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH}, orphanRemoval = true)
	private Set<ChiTietHoaDon> chiTietHoaDons= new HashSet<>();
	@OneToMany(mappedBy = "product",cascade = CascadeType.ALL)
	@JsonManagedReference
	private Set<SanPhamTonKho> sanPhamTonKhos = new HashSet<>();
	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference
	private Set<HinhAnhSanPham> hinhAnhSanPhams = new HashSet<>();

	public Product() {
		super();
	}
	public Product(Integer id, String name, String description, Float price, String mainImage, Brand brand,
			Set<ChiTietGioHang> chiTietGioHangs, Set<ChiTietHoaDon> chiTietHoaDons, Set<SanPhamTonKho> sanPhamTonKhos,
			Set<HinhAnhSanPham> hinhAnhSanPhams) {
		super();
		this.id = id;
		this.name = name;
		this.description = description;
		this.price = price;
		this.mainImage = mainImage;
		this.brand = brand;
		this.chiTietGioHangs = chiTietGioHangs;
		this.chiTietHoaDons = chiTietHoaDons;
		this.sanPhamTonKhos = sanPhamTonKhos;
		this.hinhAnhSanPhams = hinhAnhSanPhams;
	}
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Float getPrice() {
		return price;
	}
	public void setPrice(Float price) {
		this.price = price;
	}
	public String getMainImage() {
		return mainImage;
	}
	public void setMainImage(String mainImage) {
		this.mainImage = mainImage;
	}
	public Brand getBrand() {
		return brand;
	}
	public void setBrand(Brand brand) {
		this.brand = brand;
	}
	public Set<ChiTietGioHang> getChiTietGioHangs() {
		return chiTietGioHangs;
	}
	public void setChiTietGioHangs(Set<ChiTietGioHang> chiTietGioHangs) {
		this.chiTietGioHangs = chiTietGioHangs;
	}
	public Set<ChiTietHoaDon> getChiTietHoaDons() {
		return chiTietHoaDons;
	}
	public void setChiTietHoaDons(Set<ChiTietHoaDon> chiTietHoaDons) {
		this.chiTietHoaDons = chiTietHoaDons;
	}
	public Set<SanPhamTonKho> getSanPhamTonKhos() {
		return sanPhamTonKhos;
	}
	public void setSanPhamTonKhos(Set<SanPhamTonKho> sanPhamTonKhos) {
		this.sanPhamTonKhos = sanPhamTonKhos;
	}
	public Set<HinhAnhSanPham> getHinhAnhSanPhams() {
		return hinhAnhSanPhams;
	}
	public void setHinhAnhSanPhams(Set<HinhAnhSanPham> hinhAnhSanPhams) {
		this.hinhAnhSanPhams = hinhAnhSanPhams;
	}
	public void addDetail(Integer size, Integer soluong) {
	    SanPhamTonKho sanPhamTonKho = new SanPhamTonKho();
	    sanPhamTonKho.setSize(size);
	    sanPhamTonKho.setTonkho(soluong);
	    this.sanPhamTonKhos.add(sanPhamTonKho);
	}
}
