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

### [Bài Tập 3: Hệ Thống Quản Lý Tồn Kho - Cache-Aside Pattern](./BaiTap3)
- **Mục tiêu**: Thiết kế và triển khai chuẩn mực Cache-Aside Pattern cho hệ thống Quản lý Tồn kho Tiki, đảm bảo số lượng tồn kho hiển thị tức thì và chính xác khi có cập nhật.
- **Giải pháp**:
  - **Luồng Đọc (Read)**: `@Cacheable` kiểm tra Redis $\rightarrow$ Miss $\rightarrow$ Lấy từ RDBMS DB $\rightarrow$ Nạp Redis $\rightarrow$ Trả về DTO.
  - **Luồng Ghi (Write)**: Cập nhật DB trước $\rightarrow$ `@CacheEvict` hủy bỏ cache cũ đằng sau để đảm bảo tính nhất quán.
  - **Bẫy Dữ liệu (Negative Quantity)**: Fail-fast check `newQuantity < 0` ném `IllegalArgumentException`.
  - **Giảm thiểu Rủi ro Redis Evict Failure**: Áp dụng **Short TTL (5 phút)** cho cache keys kết hợp `CustomCacheErrorHandler` để dữ liệu đệm bị lỗi tự động hết hạn ngắn khi Redis gặp sự cố.
- **Báo cáo chi tiết & Sơ đồ Sequence**: [BaoCao_BaiTap3.md](./BaiTap3/BaoCao_BaiTap3.md)

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

### Bài Tập 3
```bash
cd BaiTap3
./gradlew test
```


