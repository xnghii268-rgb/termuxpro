# Termux Pro Context & Architecture

Termux Pro là ứng dụng terminal Linux độc lập phân nhánh từ termux-app và termux-packages (GPLv3).

## 1. Bố cục Thư mục (Target Folder Layout)
- `~/storage/downloads/termuxpro/src`: Mã nguồn chính (Git repo xnghii268-rgb/termuxpro).
- `~/storage/downloads/termuxpro/apk`: Lưu trữ APK phát hành (`termuxpro-v0.119.0.apk`).
- `~/storage/downloads/termuxpro/backup`: Lưu trữ bản sao lưu `.tar.gz` của home.
- `~/.secrets/termuxpro`: Keystore phát hành và GPG key signing (`termuxpro-apt-private.asc`, chmod 600).
- `~/pull-termuxpro.sh`: Tập lệnh tự động kéo APK phát hành mới nhất từ GitHub Actions qua `gh`.

## 2. Thông số Kỹ thuật & Môi trường
- **Mã định danh gói (applicationId)**: `com.termuxpro.app` (cài song song độc lập với Termux gốc).
- **Tiền tố Linux (PREFIX)**: `/data/data/com.termuxpro.app/files/usr` (Home: `/data/data/com.termuxpro.app/files/home`).
- **targetSdkVersion**: 28 | **Kiến trúc**: `arm64-v8a` | **Phiên bản**: v0.119.0 (code 121).
- **CI / Kho APT**: GitHub Actions (`build.yml`) & kho APT ký GPG `https://xnghii268-rgb.github.io/termuxpro/`.

## 3. Hệ thống Tokens Thiết kế (Design Tokens)
- **Màu thương hiệu chủ đạo**: `#FF3C70` (brand accent). Chữ trên nền accent: `on-accent` `#1A0A10` (5.60:1 WCAG AA).
- **Màu accent sáng (Light theme)**: `accent-strong` `#C8174D` (5.42:1 trên nền trắng).
- **6 chủ đề tiếng Việt**: Than đêm (`than_dem`), Mực (`muc`), Rượu vang (`ruou_vang`), Rêu đêm (`reu_dem`), Giấy (`giay`), Sương (`suong`).
- **Thanh điều hướng nổi & Bottom Inset**: 5 mục (Terminal, Phiên, Lệnh, Ảnh, Cài đặt). Ẩn mượt khi IME mở. Terminal bottom padding = runtime `mSystemNavBottomInset + navTotalHeight` (khi bàn phím đóng) và `0` (khi bàn phím mở).
- **Thanh phiên đỉnh (Session Chips)**: Tự động thụt top padding = runtime `max(statusBars, displayCutout)` chống chạm đồng hồ tai thỏ.
- **Thanh phím phụ (Extra Keys)**: Chuẩn 14 phím Termux gốc (Row 1: `ESC | / | - | HOME | UP | END | PGUP`, Row 2: `TAB | CTRL | ALT | LEFT | DOWN | RIGHT | PGDN`). Bỏ phím "Lệnh". Hỗ trợ 2 kiểu phím: "Pro" (viên kính bo 10dp, khe hở 4dp) và "Cổ điển" (thanh đen phẳng 0 viền). Tự động migration một lần. Bộ chỉnh sửa layout trong Cài đặt > Terminal.
- **Material 3 GlassSheet**: Thay thế AlertDialog bằng BottomSheet bo góc 20dp, chữ tiếng Việt sentence-case, nút nhấn rõ ràng.

## 4. Tối ưu Hiệu năng & Tương thích Linux
- **Vsync Rate Limiting**: Hạn chế vẽ lại tối đa 60 FPS qua `Choreographer.scheduleVsyncInvalidate()`, gom cụm frame khi CLI xả log nhanh.
- **Bộ đệm độ rộng ký tự Unicode**: `mCharWidthCache[0x2600]` loại bỏ hoàn toàn hàng nghìn cuộc gọi JNI `measureText` mỗi frame.
- **Nền mờ đục trên Terminal**: Thanh điều hướng và extra keys dùng màu đục tối ưu để tránh GPU re-blur mỗi khung hình.
- **Prefix Interceptor C**: Sửa lỗi macro `OLD_PREFIX_LEN` thành `sizeof(OLD_PREFIX)-1` = 21 trong `termuxpro-path.c`, hook 27 syscalls.
- **Terminfo & UTF-8**: Xuất `TERMINFO`, `TERMINFO_DIRS`, `LANG=C.UTF-8` trong Java, profile và bashrc, tránh lỗi `clear: unknown terminal`.
- **Bảng chào đón Clean Rule**: MOTD kẻ 2 đường ngang `─` trên và dưới, không viền dọc, không lệch ký tự, hiển thị thông tin thực tế.

## 5. Trạng thái Dự án
- [x] **Phase 0 (Regression Gates)**: Sửa che lấp status bar, che lấp prompt, MOTD lệch viền, bug prefix length C, terminfo.
- [x] **Phase 1 (Brand Redesign)**: Điểm nhấn `#FF3C70`, 6 bộ giao diện tiếng Việt đạt chuẩn tương phản WCAG AA >= 4.5:1.
- [x] **Phase 2 & 3 (Sheets & Motion)**: Tích hợp GlassSheet, loại bỏ icon ★ / emoji, mượt hóa IME animation và Choreographer vsync.
- [x] **Phase 4 (Packaging)**: Bump v0.119.0, cấu hình build GitHub Actions và tự động kéo APK về máy.
