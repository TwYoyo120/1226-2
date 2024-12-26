package com.example.ordermanagement.controller;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.ordermanagement.model.Brand;
import com.example.ordermanagement.model.BrandRepository;
import com.example.ordermanagement.model.CategoryRepository;
import com.example.ordermanagement.model.Item;
import com.example.ordermanagement.model.ItemOption;
import com.example.ordermanagement.model.ItemOptionRepositry;
import com.example.ordermanagement.model.ItemPhoto;
import com.example.ordermanagement.model.ItemPhotoRepositry;
import com.example.ordermanagement.model.ItemRepository;
import com.example.ordermanagement.model.UserInfo;
import com.example.ordermanagement.model.UserInfoRepository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Controller
public class HomePageController {
	
	 @Autowired
	    private ItemRepository itemRepository;
	 
	 @Autowired
	    private BrandRepository brandRepository;
	 
	 @Autowired
	 private CategoryRepository categoryRepository;
	 
	 @Autowired
	 private ItemPhotoRepositry itemPhotoRepo;
	 
	 @Autowired
	 private ItemOptionRepositry itemOptionRepo;
	 
	 @Autowired
	 private UserInfoRepository userInfoRepository;
	
	// 顯示商城首頁頁面 (http://localhost:8080/homePage)
    @GetMapping("/homePage")
    public String showhomePage() {
        return "homePage";
    }
    
    // 顯示商城搜尋頁面 (http://localhost:8080/itemSearch)
    @GetMapping("/itemSearch")
    public String searchItems(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        System.out.println("Received keyword: " + keyword); // Debug 關鍵字輸入

        List<Item> items;

        if (keyword == null || keyword.trim().isEmpty()) {
            items = itemRepository.findAll(Sort.by(Sort.Direction.DESC, "itemId"));
        } else {
            items = itemRepository.findByItemNameContainingOrBrand_BrandNameContainingOrCategory_CategoryNameContainingOrderByItemIdDesc(
                    keyword, keyword, keyword);
            System.out.println("Items found: " + items.size()); // Debug 查詢結果
        }

        model.addAttribute("items", items);
        model.addAttribute("keyword", keyword);
        return "itemSearch";
    }
    
    
    @RequestMapping("/itemSearch/filter")
    @ResponseBody
    public Page<Map<String, Object>> filterItems(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "brandIds", required = false) String brandIds,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "itemId"));

        // 保留你寫好的篩選邏輯
        Page<Item> items = itemRepository.findAll((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 價格篩選
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("itemPrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("itemPrice"), maxPrice));
            }

            // 關鍵字篩選 (商品名稱或品牌名稱)
            if (keyword != null && !keyword.trim().isEmpty()) {
                Predicate namePredicate = criteriaBuilder.like(root.get("itemName"), "%" + keyword + "%");
                Join<Item, Brand> brandJoin = root.join("brand", JoinType.LEFT);
                Predicate brandPredicate = criteriaBuilder.like(brandJoin.get("brandName"), "%" + keyword + "%");
                predicates.add(criteriaBuilder.or(namePredicate, brandPredicate));
            }

            // 分類篩選
            if (categoryId != null && categoryId > 0) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("categoryId"), categoryId));
            }

            // 品牌篩選
            if (brandIds != null && !brandIds.isEmpty()) {
                List<String> brandList = Arrays.stream(brandIds.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                if (!brandList.isEmpty()) {
                    predicates.add(root.get("brand").get("brandName").in(brandList));
                }
            }

            // 日期篩選
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("itemDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("itemDate"), endDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        // 直接讀取商品選項的價格範圍
        return items.map(item -> {
            BigDecimal minOptionPrice = item.getItemOptions()
                    .stream()
                    .map(ItemOption::getOptionPrice)
                    .filter(price -> price != null)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO); // 如果沒有選項，價格為 0

            BigDecimal maxOptionPrice = item.getItemOptions()
                    .stream()
                    .map(ItemOption::getOptionPrice)
                    .filter(price -> price != null)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            Map<String, Object> result = new HashMap<>();
            result.put("itemId", item.getItemId());
            result.put("itemName", item.getItemName());
            result.put("itemPhoto", item.getItemPhoto().isEmpty()
                    ? null
                    : encodeImage(item.getItemPhoto().get(0).getPhotoFile()));
            result.put("minPrice", minOptionPrice);
            result.put("maxPrice", maxOptionPrice);

            return result;
        });
    }
    
    @GetMapping("/items/brand/{brandName}")
    public String searchItemsByBrand(@PathVariable("brandName") String brandName, Model model) {
        // 使用 Repository 查詢商品
        List<Item> items = itemRepository.findByBrand_BrandNameContainingOrderByItemIdDesc(brandName);
        
        model.addAttribute("items", items); // 傳送查詢結果到前端
        model.addAttribute("keyword", brandName); // 傳遞品牌名稱作為搜尋關鍵字

        return "itemSearch"; // 返回搜尋結果頁面
    }
    
    
    @GetMapping("/itemPhoto/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> getItemPhoto(@PathVariable("id") Integer photoId) {
        ItemPhoto photo = itemPhotoRepo.findById(photoId).orElse(null);

        if (photo != null && photo.getPhotoFile() != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG); // 預設為 JPEG 格式
            return new ResponseEntity<>(photo.getPhotoFile(), headers, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND); // 找不到圖片返回 404
    }
    
    // 顯示商城商品頁面 (http://localhost:8080/itemDisplay)
    @GetMapping("/itemDisplay")
    public String showitemDisplay() {
        return "itemDisplay";
    }
    
    @GetMapping("/itemDisplay/{id}")
    public String showItemDisplay(@PathVariable("id") Integer itemId, Model model) {
        System.out.println("Received itemId: " + itemId);

        if (itemId == null) {
            return "redirect:/homePage"; // 如果 itemId 不是有效數字，返回首頁
        }

        // 使用 Repository 獲取商品資料
        Item item = itemRepository.findById(itemId).orElse(null);

        if (item == null) {
            return "redirect:/homePage"; // 如果商品不存在，返回首頁
        }

        // 獲取商品照片
        List<ItemPhoto> photos = itemPhotoRepo.findByItem(item);

        // 獲取商品尺寸選項
        List<ItemOption> sizeOptions = itemOptionRepo.findByItem(item);

        // 計算價格範圍
        BigDecimal minPrice = sizeOptions.stream()
                .map(ItemOption::getOptionPrice)
                .filter(price -> price != null)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice = sizeOptions.stream()
                .map(ItemOption::getOptionPrice)
                .filter(price -> price != null)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // 獲取賣家資訊
        UserInfo seller = item.getUserInfo(); // 假設 Item 有 ManyToOne 關聯到 UserInfo
        String sellerName = (seller != null) ? seller.getUserName() : "未知賣家";

        // 計算賣家商品數量
        int sellerItemCount = itemRepository.countByUserInfo(seller);

        // 加入資料至 Model
        model.addAttribute("item", item);
        model.addAttribute("photos", photos);
        model.addAttribute("sizeOptions", sizeOptions);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sellerName", sellerName); // 傳遞賣家名稱
        model.addAttribute("sellerItemCount", sellerItemCount); // 傳遞賣家商品數量

        return "itemDisplay";
    }

    
    



    
    
    
    //(http://localhost:8080/itemDisplay)
 // 取得所有品牌圖片和名稱，回傳 JSON 格式
    @GetMapping("/brands")
    @ResponseBody
    public List<BrandSimpleResponse> getAllBrands() {
        List<Brand> brands = brandRepository.findAll();
        if (brands.isEmpty()) {
            return Collections.emptyList(); // 確保空列表時返回正確格式
        }
        return brands.stream()
                     .map(brand -> new BrandSimpleResponse(
                             brand.getBrandName(),
                             encodeImage(brand.getBrandPhoto())
                     ))
                     .collect(Collectors.toList());
    }

    // 將圖片轉為 Base64 編碼，並判斷 MIME 類型
    private String encodeImage(byte[] image) {
        if (image == null) {
            return null; // 如果圖片為空，返回 null
        }

        String mimeType = isPngFormat(image) ? "image/png" : "image/jpeg";
        return "data:" + mimeType + ";base64," + java.util.Base64.getEncoder().encodeToString(image);
    }

    // 判斷圖片是否為 PNG 格式
    private boolean isPngFormat(byte[] image) {
        return image.length > 4 &&
               image[0] == (byte) 0x89 &&
               image[1] == (byte) 0x50 &&
               image[2] == (byte) 0x4E &&
               image[3] == (byte) 0x47;
    }

    // 簡化品牌數據的 DTO
    private static class BrandSimpleResponse {
        private String brandName;
        private String brandPhoto;

        public BrandSimpleResponse(String brandName, String brandPhoto) {
            this.brandName = brandName;
            this.brandPhoto = brandPhoto;
        }

        // Getters
        public String getBrandName() {
            return brandName;
        }

        public String getBrandPhoto() {
            return brandPhoto;
        }
    }
    
 // 取得所有商品分類
    @GetMapping("/categories")
    @ResponseBody
    public List<CategorySimpleResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(category -> new CategorySimpleResponse(
                        category.getCategoryId(),
                        category.getCategoryName()
                ))
                .collect(Collectors.toList());
    }

    // 簡化商品分類數據的 DTO
    private static class CategorySimpleResponse {
        private int categoryId;
        private String categoryName;

        public CategorySimpleResponse(int categoryId, String categoryName) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
        }

        public int getCategoryId() {
            return categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }
    }
    
    //(http://localhost:8080/items/latest15)
    // 取得最新 15 項商品，回傳 JSON 格式
    @GetMapping("/items/latest15")
    @ResponseBody
    public List<Map<String, Object>> getLatestItems() {
        return itemRepository.findTop15ByOrderByItemIdDesc().stream().map(item -> {
            // 計算最小價格
            BigDecimal minPrice = item.getItemOptions().stream()
                    .map(ItemOption::getOptionPrice)
                    .filter(price -> price != null)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            // 計算最大價格
            BigDecimal maxPrice = item.getItemOptions().stream()
                    .map(ItemOption::getOptionPrice)
                    .filter(price -> price != null)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            // 返回 Map 結構
            Map<String, Object> response = new HashMap<>();
            response.put("itemId", item.getItemId());
            response.put("itemName", item.getItemName());
            response.put("itemPhoto", item.getItemPhoto().isEmpty()
                    ? null
                    : encodeImage(item.getItemPhoto().get(0).getPhotoFile()));
            response.put("minPrice", minPrice); // 傳遞最小價格
            response.put("maxPrice", maxPrice); // 傳遞最大價格

            return response;
        }).collect(Collectors.toList());
    }


    
    

    // 簡化商品數據的 DTO
    private static class ItemSimpleResponse {
        private int itemId;
        private String itemName;
        private BigDecimal itemPrice;
        private String itemPhoto;

        public ItemSimpleResponse(int itemId, String itemName, BigDecimal itemPrice, String itemPhoto) {
            this.itemId = itemId;
            this.itemName = itemName;
            this.itemPrice = itemPrice;
            this.itemPhoto = itemPhoto;
        }

        // Getters
        public int getItemId() {
            return itemId;
        }

        public String getItemName() {
            return itemName;
        }

        public BigDecimal getItemPrice() {
            return itemPrice;
        }

        public String getItemPhoto() {
            return itemPhoto;
        }
    }
}