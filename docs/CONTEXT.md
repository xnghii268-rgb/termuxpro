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
- **targetSdkVersion**: 28 | **Kiến trúc**: `arm64-v8a` | **Phiên bản**: v0.119.0 (code 132).
- **CI / Kho APT**: GitHub Actions (`build.yml`) & kho APT ký GPG `https://xnghii268-rgb.github.io/termuxpro/`.

## 3. Hệ thống Giao diện Liquid Glass (Phase 6 - iOS 26/27 Inspired)
- **Bảng màu Art Direction**: Warm graphite base (`#0E0F11`), warm cream text (`#F2EDE4`), một màu accent duy nhất: Terminal mint green (`#3DDC97`), điểm xuyết champagne (`#CDB89A`).
- **Living Backdrop (`LiquidBackdropView`)**: Nền graphite với 3 vầng sáng warm-white lơ lửng, scanlines grid, và gyroscope parallax.
- **Thanh điều hướng Liquid Drop (`LiquidTabBar`)**: Viên thuốc kính mờ 4 tab (Trang chủ, Terminal, Tiện ích, Cài đặt) với giọt nước thuỷ tinh lỏng co giãn squash & stretch, touch drag & snap.
- **Trang chủ Bento Grid**: Hero Session Card tích hợp Live Terminal Preview (output thực tế từ PTY), prompt-chevron icon `>_`, triệt tiêu toàn bộ khoảng trống chết ~300px.
- **Chuẩn hóa Tiếng Việt**: Sửa toàn bộ nhãn in hoa có dấu chuẩn ("VÀO TERMINAL", "Sẵn sàng", "TRUNG TÂM TIỆN ÍCH & TÁC VỤ").

## 4. Bộ Gõ Tiếng Việt & TerminalInputConnection
- **Tài liệu phân tích chuyên sâu**: Bắt buộc tham khảo [docs/INPUT.md](file:///data/data/com.termuxpro.app/files/home/storage/downloads/termuxpro/src/docs/INPUT.md).
- **Nguyên nhân gốc rễ**: `EditorInfo.inputType` bị vô hiệu hóa composing; thiếu mirrored text context và lệch Unicode code-point khi IME xóa/thay thế ký tự.
- **Kiến trúc giải pháp**: `TerminalInputConnection` kế thừa `BaseInputConnection`, quản lý composing buffer riêng, chuẩn hóa Unicode NFC, xóa code-point chính xác, gom cụm batch edit.
- **Kiểm thử tự động**: 249 test cases trong `tests/telex_cases.json`, regression guard `tools/input_test.sh` đạt 100% pass.

## 5. Tối ưu Hiệu năng & Tương thích Linux
- **Vsync Rate Limiting**: Tối đa 60 FPS qua `Choreographer.scheduleVsyncInvalidate()`.
- **Bộ đệm độ rộng ký tự Unicode**: `mCharWidthCache[0x2600]` loại bỏ cuộc gọi JNI `measureText` mỗi frame.
- **Prefix Interceptor C**: Đánh chặn `/data/data/com.termux/`, hook `utimensat()`, `readlink()`, `readlinkat()`.
- **Terminfo & UTF-8**: Xuất `TERMINFO`, `TERMINFO_DIRS`, `LANG=C.UTF-8` trong Java, profile và bashrc.

## 6. Trạng thái Dự án
- [x] **Phase 0 - 3**: Sửa status bar, brand redesign, GlassSheet, IME animation, tối ưu Vsync.
- [x] **Phase 4 (Telex & Command Palette)**: Tái cấu trúc Bảng lệnh tương tác iOS-27 (code 131).
- [x] **Phase 5 (Core UI Redesign)**: Nâng cấp Dock nổi Liquid Glass, Safari-style Session Chips (code 132).
- [x] **Phase 6 (Liquid Glass & IME Root-Cause Engine)**: Living backdrop, LiquidTabBar drop lens, Live Terminal Preview, TerminalInputConnection chuẩn AOSP 249 cases pass.
