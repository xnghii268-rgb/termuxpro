# Termux Pro Context & Architecture

Termux Pro là ứng dụng terminal Linux độc lập phân nhánh từ termux-app và termux-packages (GPLv3).

## 1. Bố cục Thư mục Chuẩn (Target Folder Layout)
- `~/storage/downloads/termuxpro/src`: Mã nguồn chính (Source of truth, Git repo xnghii268-rgb/termuxpro).
- `~/storage/downloads/termuxpro/apk`: Lưu trữ APK phát hành (`termuxpro-v0.118.0.apk`, 38MB).
- `~/storage/downloads/termuxpro/backup`: Lưu trữ bản sao lưu `.tar.gz` của terminal home.
- `~/.build/termuxpro`: Build mirror và vùng nháp cục bộ (local scratch).
- `~/.secrets/termuxpro`: Khóa ký số phát hành (`termuxpro-release.keystore`, quyền 600, không bao giờ vào git).
- `~/pull-termuxpro.sh`: Tập lệnh tự động kéo APK phát hành mới nhất từ GitHub Actions qua `gh`.

## 2. Thông số Kỹ thuật & Môi trường
- **Mã định danh gói (applicationId)**: `com.termuxpro.app` (cài song song độc lập với Termux gốc).
- **Tiền tố Linux (PREFIX)**: `/data/data/com.termuxpro.app/files/usr`
- **Thư mục Home**: `/data/data/com.termuxpro.app/files/home`
- **targetSdkVersion**: 28 (giữ nguyên để thực thi nhị phân hợp lệ trên Android 10+).
- **Kiến trúc nhị phân**: `arm64-v8a` (tối ưu hóa kích thước APK ~38MB).
- **Môi trường CI**: GitHub Actions (tự động hóa bootstrap aarch64, patch prefix, build NDK và ký số).

## 3. Hệ thống Tokens Thiết kế (Design Tokens Summary)
- **6 chủ đề giao diện nguyên bản**: Obsidian Pro (#141517), Mocha Warm (#1E1B18), Nordic Pine (#121A1A), Slate Steel (#181B20), Paper Silk (#FBF9F5), Alabaster Frost (#F3F4F6).
- **Tương phản WCAG AA**: Tỷ lệ tương phản văn bản/nền đều đạt >= 13.3:1 (vượt chuẩn 4.5:1).
- **Bề mặt kính (Glass Session Bar)**: Translucent #EB141517, viền hairline 0.5px #26FFFFFF, bo góc chuẩn 14dp/20dp, thanh trượt trạng thái với spring animation.
- **Thanh phím tắt (Extra Keys)**: Vùng chạm >= 44dp (chiều cao 48dp), phản hồi rung Haptic Feedback.
- **Phông chữ**: Nhúng cục bộ `termuxpro-mono.ttf` (JetBrains Mono) và `termuxpro-ui.ttf` hỗ trợ 100% tiếng Việt ("Ắ Ử Ỡ ệ ợ ẫ").
- **Ngôn ngữ UI**: 100% tiếng Việt, chuẩn định dạng vi-VN.

## 4. Tính năng Cốt lõi & Dán ảnh CLI Agent
- **Thanh phiên làm việc dạng kính**: Quản lý thẻ phiên, đổi tên, gắn 5 màu thẻ (Emerald, Sapphire, Amber, Coral, Lavender), đóng nhanh có xác nhận.
- **Bảng lệnh nhanh (Command Palette)**: Tìm kiếm lịch sử lệnh shell và lệnh mẫu, chạm để chèn không tự chạy.
- **Quản lý lệnh mẫu (Snippets)**: Thêm/lưu/chèn lệnh mẫu thường dùng.
- **Sao lưu & Khôi phục**: Đóng gói tar.gz ra Downloads/termuxpro/backup, loại trừ tự động toàn bộ secret/keystore/env/key/token.
- **Dán ảnh cho AI Agent**: Nút "Dán ảnh" (tự lưu vào ~/.clip và chèn đường dẫn), "Ảnh mới nhất" (truy vấn MediaStore), nhấn giữ mở bộ chọn ảnh hệ thống, helper `clipimg` và `lastshot`, tùy chọn nén cạnh 1568px, giới hạn 30 ảnh cục bộ.
- **Tương thích Gói Upstream**: Nhúng `libtermuxpro-path.so` và cấu hình `etc/apt/apt.conf` định tuyến trong suốt đường dẫn Linux, giúp `apt` và `dpkg` hoạt động hoàn hảo trên định danh `com.termuxpro.app`.

## 5. Trạng thái Các Giai đoạn (Phase Status)
- [x] **Phase 0 - Prep (Gate 0: Passed)**: Khởi tạo thư mục, cấu hình gh, sinh keystore phát hành mới, secrets đưa lên GitHub Actions.
- [x] **Phase 1 - Fork + Rename (Gate 1: Passed)**: Đổi toàn bộ sang com.termuxpro.app, kiểm tra sạch vết.
- [x] **Phase 2 - Bootstrap CI (Gate 2: Passed)**: Tự động tải, patch prefix com.termuxpro.app, kiểm tra zero com.termux leftover.
- [x] **Phase 3 - Build + Sign CI (Gate 3: Passed)**: Biên dịch APK release, ký số tự động bằng secret Actions, upload artifact.
- [x] **Phase 4 - Verify Base (Gate 4: Passed)**: Hoàn tất cấu trúc song song, tag v1.0-base.
- [x] **Phase 5 - Theme**: 6 chủ đề JSON tương phản cao, nhúng phông chữ tiếng Việt cục bộ.
- [x] **Phase 6 - UI**: Bề mặt kính Glass Session Bar, extra-keys 48dp, 100% tiếng Việt.
- [x] **Phase 7 - Features**: Command palette, snippets, sao lưu/khôi phục an toàn.
- [x] **Phase 8 - Image Paste for CLI Agents**: Tích hợp phím dán ảnh, media store, auto-downscale, helper shell.
- [x] **Phase 9 - QA**: Kiểm tra kiểm soát chất lượng, tỉ lệ tương phản và độ tương thích.
- [x] **Phase 10 - Release**: Tải bản APK chính thức 38MB về máy, dọn sạch tiến trình nền.
