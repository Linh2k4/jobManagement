# Work Management System - JobManagement

Hệ thống quản lý công việc và đánh giá KPI (Backend Spring Boot 3.3, Java 21, PostgreSQL, Redis, MinIO, Mailpit).

## 🚀 Chạy ứng dụng bằng Docker (Khuyên dùng)

### 1. Khởi động toàn bộ hệ thống
Tại thư mục `d:\THUCTAP\Java\jobmanagement`:
```bash
docker compose up -d --build
```

Lệnh trên sẽ khởi chạy tất cả các dịch vụ:
- **Backend API (Spring Boot)**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **PostgreSQL Database**: Cổng `5432` (Database: `job_management`, User/Pass: `postgres/postgres`)
- **Redis Cache**: Cổng `6379`
- **MinIO S3 Storage Console**: `http://localhost:9001` (User/Pass: `minioadmin/minioadmin`)
- **Mailpit Email Web UI**: `http://localhost:8025`

### 2. Dừng hệ thống
```bash
docker compose down
```
*(Nếu muốn xóa cả volume dữ liệu để reset từ đầu: `docker compose down -v`)*

---

## 💻 Chạy Local (Không qua container Backend)

Nếu bạn muốn chạy backend trực tiếp bằng Maven và chỉ chạy Database/Redis/MinIO bằng Docker:

```bash
# 1. Khởi động DB, Redis, MinIO, Mailpit
docker compose up -d postgres redis minio minio-init mailpit

# 2. Chạy Spring Boot ở local
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

---

## 🔑 Tài khoản Test mặc định

Tất cả tài khoản đều có mật khẩu: `admin123`

| Email | Vai trò | Mô tả |
|---|---|---|
| `admin@company.com` | MANAGER / ADMIN | Quản trị viên hệ thống |
| `lead1@company.com` | LEAD | Trưởng nhóm 1 |
| `lead2@company.com` | LEAD | Trưởng nhóm 2 |
| `member1@company.com` | MEMBER | Nhân viên 1 |
| `member2@company.com` | MEMBER | Nhân viên 2 |

---

## 📋 Kiểm tra trạng thái Docker
```bash
# Xem các container đang chạy
docker compose ps

# Xem log của backend
docker compose logs -f backend
```
