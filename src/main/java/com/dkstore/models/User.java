package com.dkstore.models;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
	@Id
	@Column(name = "id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	@Column(name = "username", unique = true)
	private String userName;
	@Column(name = "password")
	private String passWord;
	@Column(name = "enabled")
	private Boolean enabled = false;
	@Column(name = "fullname")
	private String fullName;
	@Column(name = "gender")
	private String gender;
	@Column(name = "birthday")
	private Date birthday;
	@Column(name = "address")
	private String address;
	@Column(name = "email", unique = true)
	private String email;
	@Column(name = "telephone")
	private String telephone;
	
	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private GioHang gioHangs;
	
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<HoaDon> hoaDons = new ArrayList<>();
		
	@OneToMany(mappedBy = "user", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private Set<UserRole> userRoles = new HashSet<>();
	
	public User() {
		// TODO Auto-generated constructor stub
	}

	public User(Integer id, String userName, String passWord, Boolean enabled, String fullName, String gender,
			Date birthday, String address, String email, String telephone, GioHang gioHangs, List<HoaDon> hoaDons,
			Set<UserRole> userRoles) {
		super();
		this.id = id;
		this.userName = userName;
		this.passWord = passWord;
		this.enabled = enabled;
		this.fullName = fullName;
		this.gender = gender;
		this.birthday = birthday;
		this.address = address;
		this.email = email;
		this.telephone = telephone;
		this.gioHangs = gioHangs;
		this.hoaDons = hoaDons;
		this.userRoles = userRoles;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassWord() {
		return passWord;
	}

	public void setPassWord(String passWord) {
		this.passWord = passWord;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public Date getBirthday() {
		return birthday;
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getTelephone() {
		return telephone;
	}

	public void setTelephone(String telephone) {
		this.telephone = telephone;
	}

	public GioHang getGioHangs() {
		return gioHangs;
	}

	public void setGioHangs(GioHang gioHangs) {
		this.gioHangs = gioHangs;
	}

	public List<HoaDon> getHoaDons() {
		return hoaDons;
	}

	public void setHoaDons(List<HoaDon> hoaDons) {
		this.hoaDons = hoaDons;
	}

	public Set<UserRole> getUserRoles() {
		return userRoles;
	}

	public void setUserRoles(Set<UserRole> userRoles) {
		this.userRoles = userRoles;
	}
}
