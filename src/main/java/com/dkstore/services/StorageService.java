package com.dkstore.services;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
	String store(MultipartFile file);
    void init();
    List<String> storeMultiple(MultipartFile[] files);
}
