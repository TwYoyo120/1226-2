package com.example.ordermanagement.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.ordermanagement.model.Item;
import com.example.ordermanagement.model.ItemRepository;
import com.example.ordermanagement.model.Category;
import com.example.ordermanagement.model.CategoryRepository;

@Service
public class CategoryService {
    
    
    @Autowired
    private CategoryRepository categoryRepo; // 引入 CategoryRepository
    
    public List<Category> findAll() {
        return categoryRepo.findAll();
    }

    public Category addCategory(Category category) {
    	return categoryRepo.save(category);
    }
    
    
    
}