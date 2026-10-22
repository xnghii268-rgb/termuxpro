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
- **targetSdkVersion**: 28 | **Kiến trúc**: `arm64-v8a` | **Phiên bản**: v0.119.0 (code 131).
- **CI / Kho APT**: GitHub Actions (`build.yml`) & kho APT ký GPG `https://xnghii268-rgb.github.io/termuxpro/`.

## 3. Hệ thống Giao diện & Trải nghiệm Người dùng
- **Màu thương hiệu chủ đạo**: `#FF3C70` (brand accent). Chữ trên nền accent: `on-accent` `#1A0A10` (5.60:1 WCAG AA).
- **6 chủ đề tiếng Việt**: Than đêm (`than_dem`), Mực (`muc`), Rượu vang (`ruou_vang`), Rêu đêm (`reu_dem`), Giấy (`giay`), Sương (`suong`).
- **Thanh điều hướng nổi & Bottom Inset**: 5 mục (Terminal, Phiên, Lệnh, Ảnh, Cài đặt). Ẩn mượt khi IME mở. Terminal bottom padding = runtime `mSystemNavBottomInset + navTotalHeight` (bàn phím đóng) và `0` (bàn phím mở).
- **Thanh phiên đỉnh (Session Chips)**: Thụt top padding = runtime `max(statusBars, displayCutout)` chống chạm tai thỏ.
- **Thanh phím phụ (Extra Keys)**: Chuẩn 14 phím Termux gốc, 2 kiểu "Pro" và "Cổ điển", tùy biến trong Cài đặt.
- **Bảng lệnh tương tác (Command Palette)**: Chuẩn iOS-27 cao cấp với RecyclerView nested scrolling, segmented control (Lịch sử | Mẫu lệnh), ghim tối đa 5 lệnh, lịch sử giới hạn 100 mục có bảo lưu lệnh ghim, cử chỉ vuốt xoá/ghim và tìm kiếm thời gian thực.

## 4. Tối ưu Hiệu năng, Bộ gõ & Tương thích Linux
- **Vsync Rate Limiting**: Tối đa 60 FPS qua `Choreographer.scheduleVsyncInvalidate()`, gom cụm frame xả log.
- **Bộ đệm độ rộng ký tự Unicode**: `mCharWidthCache[0x2600]` loại bỏ cuộc gọi JNI `measureText` mỗi frame.
- **Bộ gõ Tiếng Việt Telex Engine**: Khắc phục lặp ký tự `dowwn`, bảo vệ 100% từ khóa Linux/English (`down`, `status`, `grep`, ...), đầy đủ nguyên âm đôi/ba và dấu thanh.
- **Prefix Interceptor C**: Đánh chặn đường dẫn `/data/data/com.termux/`, bổ sung hook `utimensat()`, `readlink()`, `readlinkat()` cho dpkg.
- **Terminfo & UTF-8**: Xuất `TERMINFO`, `TERMINFO_DIRS`, `LANG=C.UTF-8` trong Java, profile và bashrc.
- **Bảng chào đón Clean Rule**: MOTD kẻ 2 đường ngang `─` trên và dưới, hiển thị thông tin thực tế.

## 5. Trạng thái Dự án
- [x] **Phase 0 (Regression Gates)**: Sửa status bar, che prompt, MOTD lệch viền, bug prefix C, terminfo.
- [x] **Phase 1 (Brand Redesign)**: Điểm nhấn `#FF3C70`, 6 bộ giao diện tiếng Việt đạt chuẩn WCAG AA >= 4.5:1.
- [x] **Phase 2 & 3 (Sheets & Motion)**: Tích hợp GlassSheet, IME animation, tối ưu Choreographer vsync.
- [x] **Phase 4 (Telex & Command Palette)**: Khắc phục bộ gõ tiếng Việt Telex, tái cấu trúc Bảng lệnh tương tác iOS-27, unit test 100% pass, bump versionCode 131.
