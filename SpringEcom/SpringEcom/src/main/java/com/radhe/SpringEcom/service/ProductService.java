package com.radhe.SpringEcom.service;


import com.radhe.SpringEcom.model.Products;
import com.radhe.SpringEcom.repo.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepo productRepo;

    public List<Products> getAllProducts() {
        return productRepo.findAll();

    }



    public Products getProductById(int id) {
        return productRepo.findById(id).orElse(null);   // .get() ki jagah, warna 404 ki jagah 500 aata hai
    }

    public Products addProduct(Products product, MultipartFile imageFile) throws IOException {

        product.setImageName(imageFile.getOriginalFilename());
        product.setImageType(imageFile.getContentType());
        product.setImageData(imageFile.getBytes());

        return productRepo.save(product);
    }



    public Products updateProduct(Products product, MultipartFile imageFile) throws IOException {
        if (imageFile != null && !imageFile.isEmpty()) {
            product.setImageName(imageFile.getOriginalFilename());
            product.setImageType(imageFile.getContentType());
            product.setImageData(imageFile.getBytes());
        } else {
            // image nahi aayi, to purani image rakho
            Products existing = productRepo.findById(product.getId()).orElseThrow();
            product.setImageName(existing.getImageName());
            product.setImageType(existing.getImageType());
            product.setImageData(existing.getImageData());
        }
        return productRepo.save(product);
    }

    public void deleteProduct(int id) {
        productRepo.deleteById(id);
    }

    public List<Products> searchProducts(String keyword) {
        return productRepo.searchProducts(keyword);
    }

    // NEW: dropdown ke liye categories
    public List<String> getCategories() {
        return productRepo.findDistinctCategories();
    }
}
