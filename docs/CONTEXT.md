# Termux Pro Context & Architecture

Termux Pro là ứng dụng terminal Linux mã nguồn mở (GPLv3) phân nhánh từ termux-app, độc lập và chuyên biệt cho thiết bị Android di động.

## 1. Bố cục Thư mục Chuẩn (Target Folder Layout)
- `~/storage/downloads/termuxpro/src`: Mã nguồn chính (Source of truth).
- `~/storage/downloads/termuxpro/apk`: Thư mục chứa tệp APK phát hành duy nhất (`termuxpro-v<version>.apk`).
- `~/storage/downloads/termuxpro/backup`: Thư mục lưu trữ bản sao lưu `.tar.gz` của terminal home.
- `~/.build/termuxpro`: Build mirror và vùng nháp cục bộ (local scratch).
- `~/.secrets/termuxpro`: Chứng chỉ ký số (`termuxpro-release.keystore`, `keystore.env`), bảo mật 100%, quyền 600.
- `~/pull-termuxpro.sh`: Tập lệnh tự động tải APK phát hành mới nhất từ GitHub Actions qua `gh`.

## 2. Thông số Kỹ thuật & Môi trường
- **Mã định danh gói (applicationId)**: `com.termuxpro.app`
- **Tiền tố Linux (PREFIX)**: `/data/data/com.termuxpro.app/files/usr`
- **Thư mục Home**: `/data/data/com.termuxpro.app/files/home`
- **targetSdkVersion**: 28 (giữ nguyên để hỗ trợ W^X trên Android 10+).
- **Môi trường build**: GitHub Actions (aarch64 bootstrap & APK build).

## 3. Hệ thống Tokens Thiết kế (Design Tokens Summary)
- **Bảng màu**: 6 chủ đề gốc (2 sáng, 4 tối) đạt chuẩn tương phản WCAG 2.1 >= 4.5:1.
- **Bề mặt kính (Glass Surface)**: Mờ nền thích ứng 18px, viền 0.5px hairline, hỗ trợ fallback mờ đục trên thiết bị cấu hình thấp.
- **Thanh phím tắt (Extra Keys)**: Vùng chạm công thái học >= 44dp, tích hợp rung phản hồi haptic.
- **Typography**: Nhúng cục bộ 100% phông chữ hỗ trợ đầy đủ tiếng Việt ("Ắ Ử Ỡ ệ ợ ẫ").

## 4. Tính năng Cốt lõi
- Thanh phiên làm việc (Session Bar) dạng kính thay thế drawer phẳng truyền thống.
- Command Palette tìm kiếm lịch sử lệnh và snippet có sẵn.
- Trình quản lý sao lưu / khôi phục tar.gz loại trừ các thư mục bí mật.
- Tính năng Dán ảnh thông minh (Image Paste) dành cho CLI AI Agents: nút "Dán ảnh" (tự động lưu vào `~/.clip/` và chèn đường dẫn), "Ảnh mới nhất" (truy vấn MediaStore), helper `clipimg`, `lastshot`.

## 5. Trạng thái Các Giai đoạn (Phase Status)
- [x] **Phase 0 - Prep**: Thiết lập thư mục, cài đặt gh/jq, sinh keystore phát hành mới, tạo `pull-termuxpro.sh`.
- [ ] **Gate 0**: Đang chờ nạp `GITHUB_TOKEN` để kết nối kho lưu trữ từ xa và thiết lập GitHub Actions.
- [ ] **Phase 1**: Fork & Đổi tên package sang `com.termuxpro.app`.
- [ ] **Phase 2**: CI Bootstrap aarch64 workflow.
- [ ] **Phase 3**: CI Build & Sign APK workflow.
- [ ] **Phase 4 - 10**: Verify, Theme, UI, Features, Image Paste, QA & Release.
