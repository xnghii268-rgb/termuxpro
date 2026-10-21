# Quản lý & Biên dịch Gói trong Kho APT Termux Pro

## 1. Giới thiệu Kho Gói Riêng (Independent APT Repository)
Termux Pro sử dụng kho gói APT độc lập, có chữ ký số GPG (`apt@termuxpro.app`) riêng biệt, được lưu trữ tự động trên GitHub Pages:
- **Địa chỉ kho**: `https://xnghii268-rgb.github.io/termuxpro/`
- **Tập tin cấu hình**: `/data/data/com.termuxpro.app/files/usr/etc/apt/sources.list`
- **Khóa xác thực**: `/data/data/com.termuxpro.app/files/usr/etc/apt/trusted.gpg.d/termuxpro.gpg`

## 2. Cách Biên dịch Thêm Gói Mới (Workflow Dispatch)
Khi cần bổ sung bất kỳ gói phần mềm nào vào kho APT Termux Pro, bạn có thể kích hoạt trực tiếp từ giao diện GitHub Actions hoặc thông qua GitHub CLI `gh`:

### Cách 1: Qua GitHub CLI trên Terminal
```bash
gh workflow run "Build Termux Pro Release APK & Packages" -f packages="python git nodejs-lts openssh htop"
```

### Cách 2: Qua Giao diện Web GitHub
1. Truy cập: `https://github.com/xnghii268-rgb/termuxpro/actions`
2. Chọn workflow **"Build Termux Pro Release APK & Packages"** ở thanh bên trái.
3. Bấm nút **"Run workflow"** (bên phải).
4. Nhập danh sách tên các gói cần biên dịch vào ô `packages` (ví dụ: `python git nodejs-lts`).
5. Bấm **"Run workflow"**.

Hệ thống CI sẽ tự động:
1. Biên dịch các gói từ mã nguồn với tiền tố `/data/data/com.termuxpro.app/files/usr`.
2. Tạo tệp siêu dữ liệu `Packages` và `Packages.gz`.
3. Ký số `Release.gpg` và `InRelease` bằng khóa bí mật GPG của Termux Pro.
4. Tự động triển khai lên nhánh `gh-pages` để phục vụ người dùng.

## 3. Cài đặt trên Thiết bị
Sau khi CI hoàn tất, người dùng trên Termux Pro chỉ cần chạy:
```bash
pkg update
pkg install <tên-gói>
```
Gói phần mềm sẽ được tải trực tiếp từ kho riêng Termux Pro và chạy nguyên bản 100% không gặp bất kỳ xung đột sandbox nào.
