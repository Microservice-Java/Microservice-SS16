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

## Hướng dẫn chạy và kiểm thử

### Bài Tập 1
```bash
cd BaiTap1
./gradlew test
```
