# Microservice SS16 - Caching Strategies, Distributed Cache & Resiliency

Repository lưu trữ bài tập thực hành về **Bộ nhớ đệm phân tán (Distributed Cache)**, **Redis**, và **Chiến lược xử lý lỗi (Fault Tolerance)** trong hệ thống Microservices.

---

## Danh sách bài tập

### [Bài Tập 1: Vá Lỗi "Bán Giá Không Đồng Nhất" - Local Cache vs Distributed Cache](./BaiTap1)
- **Mục tiêu**: Phân tích và khắc phục lỗi chênh lệch giá sản phẩm Flash Sale giữa 10 instances do sử dụng `HashMap` local cache độc lập trên từng JVM.
- **Giải pháp**:
  - **Tái thiết kế bộ nhớ đệm**: Chuyển đổi toàn bộ bộ nhớ đệm cục bộ sang **Redis Distributed Cache** đính kèm `@Cacheable` và `@CacheEvict`.
  - **Cơ chế Chịu lỗi (Fault Tolerance)**: Đăng ký `CustomCacheErrorHandler` implements `CacheErrorHandler` để lắng nghe mọi ngoại lệ ngắt kết nối Redis (`RedisConnectionFailureException`, timeout), tự động log cảnh báo và fallback truy vấn trực tiếp từ Cơ sở dữ liệu (Database) mà không làm sập ứng dụng.
  - **Kiểm soát Tham số đầu vào**: Áp dụng cơ chế Fail-fast với `IllegalArgumentException` cho `productId` null hoặc rỗng và đính kèm `condition = "#productId != null && !#productId.trim().isEmpty()"` vào annotation đệm để chặn tạo rác key trên Redis.
- **Báo cáo chi tiết & Visual Diagram**: [BaoCao_BaiTap1.md](./BaiTap1/BaoCao_BaiTap1.md)

---

### [Bài Tập 2: Khắc Phục Lỗi "Cache Không Hoạt Động" - Thiếu @EnableCaching](./BaiTap2)
- **Mục tiêu**: Phân tích nguyên nhân kỹ thuật khiến `@Cacheable` bị vô hiệu hóa do thiếu `@EnableCaching` trên Spring Application làm ngắt kết nối Spring AOP Proxy và `CacheInterceptor`.
- **Giải pháp**:
  - **Kích hoạt Spring Cache AOP Engine**: Bổ sung `@EnableCaching` tại `UserApplication.java` và đăng ký `ConcurrentMapCacheManager` trong `CacheConfig.java`.
  - **Kiểm soát Tham số & Kết quả Null**: Cấu hình `@Cacheable(value = "users", key = "#userId", condition = "#userId != null && !#userId.trim().isEmpty()", unless = "#result == null")` trên `UserService.getUserById()` giúp chống lãng phí RAM và tránh lưu cache kết quả `null`.
  - **Fail-Fast Validation**: Tự động quăng `IllegalArgumentException` khi `userId` null hoặc chuỗi rỗng.
- **Báo cáo chi tiết & Phân tích AOP**: [BaoCao_BaiTap2.md](./BaiTap2/BaoCao_BaiTap2.md)

---

## Hướng dẫn chạy và kiểm thử

### Bài Tập 1
```bash
cd BaiTap1
./gradlew test
```

### Bài Tập 2
```bash
cd BaiTap2
./gradlew test
```

