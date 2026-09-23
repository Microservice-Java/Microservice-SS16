# BÁO CÁO PHÂN TÍCH LỖI: KHẮC PHỤC LỖI "CACHE KHÔNG HOẠT ĐỘNG" – THIẾU @ENABLECACHING

**Họ và tên:** Rika & Team  
**Khóa học:** Microservices Architecture & Java Spring Boot  
**Bài tập:** Bài tập 2 - Session 16  
**Repository:** [https://github.com/Microservice-Java/Microservice-SS16.git](https://github.com/Microservice-Java/Microservice-SS16.git)

---

## 1. Phân tích Nguyên nhân Kỹ thuật: Tại sao Cache không hoạt động?

### 1.1. Bối cảnh Sự cố
Hệ thống Fintech xử lý 10.000 request/giây gặp phải độ trễ phản hồi cao (200ms) trên API tra cứu thông tin người dùng `getUserById`. Mặc dù đội ngũ phát triển đã khai báo `@Cacheable(value = "users", key = "#userId")` trên `UserService`, mọi request gửi tới hệ thống vẫn luôn thực thi câu lệnh SQL truy xuất Database.

### 1.2. Đoạn mã nguồn bị thiếu cấu hình (Legacy Code)
```java
// Application.java - Thiếu annotation kích hoạt Caching Engine!
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 1.3. Cơ chế Spring AOP Proxy & CacheInterceptor
Spring Cache hoạt động dựa trên mô hình **Spring AOP (Aspect-Oriented Programming) Proxy**:
1. **Thiếu `@EnableCaching`:** Annotation `@SpringBootApplication` mặc định **không** tự động bật tính năng Caching của Spring. Khi không có `@EnableCaching`, Spring IoC Container không đăng ký các Bean hạ tầng quan trọng như `BeanFactoryCacheOperationSourceAdvisor`, `CacheOperationSource`, và `CacheInterceptor`.
2. **Không khởi tạo AOP Proxy:** Do thiếu Advisor, Spring Boot bỏ qua các annotation đệm `@Cacheable`, `@CacheEvict`, `@CachePut` và khởi tạo `UserService` như một Java Bean thông thường (không được bọc bởi AOP Dynamic Proxy hay CGLIB Proxy).
3. **Kết quả:** Khi ứng dụng gọi `userService.getUserById(userId)`, phương thức thực thi trực tiếp mã nguồn bên trong mà không đi qua `CacheInterceptor`. Do đó, dòng code `userRepository.findById(userId)` luôn chạy, khiến độ trễ giữ ở mức 200ms thay vì < 20ms.

---

## 2. Giải pháp Sửa lỗi & Kiến trúc Mã nguồn

### 2.1. Khai báo `@EnableCaching` và cấu hình `CacheManager`
Bổ sung `@EnableCaching` tại `UserApplication.java` và đăng ký `ConcurrentMapCacheManager` trong lớp `CacheConfig.java`:

```java
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String USERS_CACHE = "users";

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(USERS_CACHE);
        cacheManager.setAllowNullValues(false); // Không cho phép lưu null value mặc định
        return cacheManager;
    }
}
```

### 2.2. Cập nhật `UserService.java` với Điều kiện Caching & Fail-Fast
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Cacheable(
        value = CacheConfig.USERS_CACHE,
        key = "#userId",
        condition = "#userId != null && !#userId.trim().isEmpty()",
        unless = "#result == null"
    )
    public User getUserById(String userId) {
        validateUserId(userId);
        System.out.println(">>> Truy vấn Database cho userId: " + userId);
        log.info("[DB Query] Fetching User from Database for userId: {}", userId);

        return userRepository.findById(userId).orElse(null);
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID must not be null or blank");
        }
    }
}
```

---

## 3. Phân tích & Đề xuất Xử lý các Tình huống Đặc biệt

### 3.1. Tình huống Tham số `userId` Truyền vào là Null hoặc Rỗng
- **Rủi ro:** Nếu không chặn, Spring Cache có thể tạo ra key rác (`users::null` hoặc `users::`) trên bộ nhớ đệm, làm lãng phí RAM và gây lỗi logic.
- **Giải pháp:** 
  1. Kiểm tra fail-fast ngay ở đầu hàm (`validateUserId`) để quăng `IllegalArgumentException`.
  2. Kết hợp với `condition = "#userId != null && !#userId.trim().isEmpty()"` trên annotation đệm để Spring bypass cache hoàn toàn khi tham số không hợp lệ.

### 3.2. Tình huống Phương thức Trả về Null (User Không Tồn Tại)
Có nên lưu giá trị `null` vào Cache không?

| Chiến lược | Ưu điểm | Nhược điểm | Trường hợp áp dụng |
|---|---|---|---|
| **Lưu giá trị `null` vào Cache** (`allowNullValues = true`) | Chống tấn công **Cache Penetration** (khi kẻ xấu cố tình gửi 10.000 req/s với ID giả để đánh sập DB). | Nếu người dùng tạo tài khoản mới sau đó, hệ thống vẫn trả về `null` từ Cache cho đến khi TTL hết hạn hoặc cache bị evict. | Các hệ thống đọc dữ liệu tĩnh, ít tạo mới tài khoản liên tục. |
| **Không lưu `null` vào Cache** (`unless = "#result == null"`) | Ngay khi user mới được tạo trong DB, truy vấn kế tiếp sẽ lấy được dữ liệu mới nhất mà không bị vướng đệm `null`. | Nếu bị tấn công đọc ID giả liên tục, DB vẫn phải chịu tải cho từng request. | Các hệ thống Fintech/Ngân hàng nơi tài khoản người dùng được tạo mới liên tục và cần dữ liệu nhất quán. |

> **Khuyên dùng cho Fintech API:** Sử dụng `unless = "#result == null"` để không lưu kết quả `null`, kết hợp với RateLimiter ở API Gateway để ngăn chặn tấn công Cache Penetration.

---

## 4. Hướng dẫn Cài đặt & Kết quả Chạy Kiểm thử

### Lệnh chạy test
```bash
cd SS16/BaiTap2
./gradlew test
```

### Kết quả Kiểm thử (`UserServiceCacheTest.java`)
- **Test 1 (`testCacheHitMiss_ProvesCachingIsActive`)**: PASSED  
  * Lần gọi 1 (Cache Miss): Thực thi `userRepository.findById("USER001")` (In log `>>> Truy vấn Database cho userId: USER001`).  
  * Lần gọi 2 (Cache Hit): Lấy trực tiếp từ `ConcurrentMapCacheManager`. Tổng số lần gọi DB giữ nguyên = 1 (`verify(userRepository, times(1)).findById("USER001")`).
- **Test 2 (`testUnlessNull_DoesNotCacheNullResult`)**: PASSED  
  * Lần 1 trả về `null`, lần 2 truy xuất DB lại đúng theo điều kiện `unless = "#result == null"`.
- **Test 3 (`testInputValidation_NullOrBlankUserId`)**: PASSED  
  * Ném `IllegalArgumentException` fail-fast khi `userId` null hoặc rỗng.

---

## 5. Kết luận
Lỗi "Cache không hoạt động" đã được khắc phục hoàn toàn bằng việc bổ sung `@EnableCaching`, giúp Spring AOP kích hoạt `CacheInterceptor`. Hệ thống Fintech đạt được tốc độ phản hồi < 20ms cho các truy vấn Cache Hit, đồng thời đảm bảo an toàn bộ nhớ đệm và tính đúng đắn khi xử lý các giá trị null.
