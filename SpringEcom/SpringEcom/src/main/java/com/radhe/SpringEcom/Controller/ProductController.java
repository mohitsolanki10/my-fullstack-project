package com.radhe.SpringEcom.Controller;


import com.radhe.SpringEcom.model.Products;
import com.radhe.SpringEcom.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/products")
    public ResponseEntity<List<Products>> getProducts(){
        return new ResponseEntity<>(productService.getAllProducts(),HttpStatus.OK);
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<Products> getProductById(@PathVariable int id){

        if(productService.getProductById(id) != null)
            return new ResponseEntity<>(productService.getProductById(id),HttpStatus.OK);
        else
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

    }



    @GetMapping("/product/{productId}/image")
    public ResponseEntity<byte[]> getImageByProductId(@PathVariable int productId) {
        Products product = productService.getProductById(productId);
        if (product == null || product.getImageData() == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        product.getImageType() != null ? product.getImageType() : "image/jpeg"))
                .body(product.getImageData());
    }



    @PostMapping("/product")
    public ResponseEntity<?> addProduct(@RequestPart Products product, @RequestPart MultipartFile imageFile) {
        Products savedProduct = null;
        try {
            savedProduct = productService.addProduct(product, imageFile);
            return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
        } catch (IOException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PutMapping("/product/{id}")
    public ResponseEntity<String> updateProduct(@PathVariable int id,
                                                @RequestPart Products product,
                                                @RequestPart(required = false) MultipartFile imageFile) {
        try {
            product.setId(id);
            productService.updateProduct(product, imageFile);
            return new ResponseEntity<>("Updated..", HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/product/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable int id){
        Products product = productService.getProductById(id);
        if(product != null){
            productService.deleteProduct(id);
            return new ResponseEntity<>("Deleted..",HttpStatus.OK);
        }else{
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    // NEW: navbar ke category dropdown ke liye
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories(){
        return new ResponseEntity<>(productService.getCategories(), HttpStatus.OK);
    }

    @GetMapping("/products/search")
    public ResponseEntity<List<Products>> searchProducts(@RequestParam String keyword){
        List<Products> products = productService.searchProducts(keyword);
        System.out.println("Search With " + keyword);
        return new ResponseEntity<>(products,HttpStatus.OK);

    }

}





