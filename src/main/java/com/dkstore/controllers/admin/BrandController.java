package com.dkstore.controllers.admin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.springframework.web.multipart.MultipartFile;

import com.dkstore.models.Brand;
import com.dkstore.models.Country;
import com.dkstore.models.Product;
import com.dkstore.services.BrandService;
import com.dkstore.services.StorageService;

@Controller
@RequestMapping("/admin")
public class BrandController {
	@Autowired
	private BrandService brandService;
	@Autowired
	private StorageService storageService;

	@GetMapping("/brand")
	public String index(Model model, @RequestParam(defaultValue = "1") Integer pageNo,@Param("keyword") String keyword) {
		Page<Brand> listBrands = this.brandService.getAll(pageNo);
		if(keyword != null) {
			listBrands = this.brandService.search(keyword,pageNo);
			model.addAttribute("keyword",keyword);
		}
		model.addAttribute("list", listBrands);
		model.addAttribute("totalPage", listBrands.getTotalPages());
		model.addAttribute("currentPage", pageNo);
		return "admin/brand/index";
	}

	@GetMapping("/add-brand")
	public String add(Model model) {
		Brand brand = new Brand();
		brand.setStatus(true);
		model.addAttribute("brand", brand);
		model.addAttribute("country", Country.values());
		return "admin/brand/add";
	}

	@PostMapping("/add-brand")
	public String save(@ModelAttribute Brand brand, @RequestParam("logo_url1") MultipartFile file) {
		String filePath = this.storageService.store(file);
		brand.setLogo_url(filePath);
		if (this.brandService.create(brand)) {
			return "redirect:/admin/brand";
		} else {
			return "admin/brand/add";
		}
	}

	@GetMapping("/edit-brand/{id}")
	public String edit(Model model, @PathVariable Integer id) {
		Brand brand = this.brandService.findById(id);
		model.addAttribute("brand", brand);
		model.addAttribute("country", Country.values());
		return "admin/brand/edit";
	}

	@PostMapping("/edit-brand")
	public String update(@ModelAttribute Brand brand, @RequestParam("logo_url1") MultipartFile file) {
		Brand existBrand = this.brandService.findById(brand.getId());
		if (existBrand == null) {
			return "redirect:/admin/brand";
		}

		if (file != null && !file.isEmpty()) {
			String filePath = this.storageService.store(file);
			brand.setLogo_url(filePath);
		} else {
			brand.setLogo_url(existBrand.getLogo_url());
		}

		if (this.brandService.update(brand)) {
			return "redirect:/admin/brand";
		}
		return "admin/brand/edit";
	}

	@GetMapping("/delete-brand/{id}")
	public String delete(@PathVariable Integer id) {
		Brand existBrand = this.brandService.findById(id);
		if (existBrand != null) {
			if (this.brandService.delete(id)) {
				return "redirect:/admin/brand";
			}
		}
		return "redirect:/admin/brand";
	}
}
