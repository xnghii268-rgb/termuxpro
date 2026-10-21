# Termux Pro Context & Architecture

Termux Pro là ứng dụng terminal Linux độc lập phân nhánh từ termux-app và termux-packages (GPLv3).

## 1. Bố cục Thư mục Chuẩn (Target Folder Layout)
- `~/storage/downloads/termuxpro/src`: Mã nguồn chính (Source of truth, Git repo xnghii268-rgb/termuxpro).
- `~/storage/downloads/termuxpro/apk`: Lưu trữ APK phát hành (`termuxpro-v0.118.0.apk`, 38MB).
- `~/storage/downloads/termuxpro/backup`: Lưu trữ bản sao lưu `.tar.gz` của terminal home.
- `~/.build/termuxpro`: Build mirror và vùng nháp cục bộ (local scratch).
- `~/.secrets/termuxpro`: Keystore phát hành và GPG key signing (`termuxpro-apt-private.asc`, quyền 600).
- `~/pull-termuxpro.sh`: Tập lệnh tự động kéo APK phát hành mới nhất từ GitHub Actions qua `gh`.

## 2. Thông số Kỹ thuật & Môi trường
- **Mã định danh gói (applicationId)**: `com.termuxpro.app` (cài song song độc lập với Termux gốc).
- **Tiền tố Linux (PREFIX)**: `/data/data/com.termuxpro.app/files/usr`
- **Thư mục Home**: `/data/data/com.termuxpro.app/files/home`
- **targetSdkVersion**: 28 (giữ nguyên để thực thi nhị phân hợp lệ trên Android 10+).
- **Kiến trúc nhị phân**: `arm64-v8a` (tối ưu hóa kích thước APK ~38MB).
- **Môi trường CI**: GitHub Actions (tự động hóa bootstrap aarch64, patch prefix, build NDK và ký số).
- **Kho APT độc lập**: `https://xnghii268-rgb.github.io/termuxpro/` (ký số GPG `apt@termuxpro.app`).

## 3. Hệ thống Tokens Thiết kế (Design Tokens Summary)
- **6 chủ đề giao diện nguyên bản**: Obsidian Pro (#141517), Mocha Warm (#1E1B18), Nordic Pine (#121A1A), Slate Steel (#181B20), Paper Silk (#FBF9F5), Alabaster Frost (#F3F4F6). Tương phản WCAG AA >= 13.3:1.
- **Thanh điều hướng nổi Liquid Glass**: 5 mục (Terminal, Phiên, Lệnh, Ảnh, Cài đặt). Tự động trượt ẩn mượt mà (WindowInsetsCompat 200ms) khi bàn phím ảo (IME) mở hoặc khi kết nối bàn phím cứng / thanh chọn văn bản, giải phóng toàn bộ chiều cao cho terminal.
- **Thanh phiên dạng thẻ (Top Session Chips)**: Nằm trên đỉnh terminal, hiển thị chấm màu trạng thái và nút thêm phiên nhanh.
- **Thanh phím tắt (Extra Keys)**: Kiểu kính mờ đồng nhất (bo góc 10dp, khoảng cách 4dp, viền 0.5dp hairline, scale 0.97 khi nhấn, không bao giờ bị đen cứng). Nhãn 1 dòng autoSize (10-14sp) vừa vặn trên màn hình 360/390/412dp, phím đặc biệt và toggle (Ctrl/Alt) có màu nhấn tương ứng.
- **Bảng chào đón Hacker Console**: Khung hộp ╭─╮ ╰─╯, hiển thị pin, IP, giờ, uptime, menu [1]-[9], [0], 10 mẹo tiếng Việt (<20ms).
- **Phông chữ & Ngôn ngữ**: Nhúng `termuxpro-mono.ttf` và `termuxpro-ui.ttf`, hỗ trợ 100% tiếng Việt vi-VN.

## 4. Tính năng Cốt lõi & Tương thích Sandbox
- **Chống lỗi Sandbox Permission Denied**: Xuất môi trường `SSL_CERT_FILE`, `CURL_CA_BUNDLE`, `OPENSSL_CONF`, `GIT_SSL_CAINFO` và thư viện chặn syscall `libtermuxpro-path.so`.
- **Hỗ trợ AntiGravity CLI**: Sửa lỗi trust anchors, script `curl -fsSL https://antigravity.google/cli/install.sh | bash` cài đặt thành công 100%.
- **Dán ảnh đa phương tiện (Rich Content / Gboard)**: Hỗ trợ dán ảnh trực tiếp từ clipboard Gboard (`EditorInfoCompat` + `InputConnectionCompat`) và phím nhanh "Dán ảnh" / "Ảnh mới", tự động lưu `~/.clip/` (giữ 30 ảnh gần nhất), dán đường dẫn trực tiếp vào dòng lệnh.
- **Khởi động Login Shell Chuẩn**: Thiết lập sẵn `~/.bashrc` & `~/.profile`, bảo toàn `libtermuxpro-path.so` trong `LD_PRELOAD`, hiển thị prompt chuẩn `~ $` màu cyan thay vì `bash-5.3$`.
- **Bảng lệnh (Command Palette) & Sao lưu**: Tra cứu lịch sử lệnh, snippets, đóng gói sao lưu loại trừ secrets.

## 5. Trạng thái Các Giai đoạn
- [x] **Phase F1 - Diagnosis (Gate F1: Passed)**: Phân loại 100% đường dẫn com.termux trong rootfs.
- [x] **Phase F2 - Rebuild Prefix & Repo (Gate F2: Passed)**: Kho APT riêng ký GPG trên GitHub Pages, MOTD tiếng Việt, overlay bootstrap.
- [x] **Phase U - UI Redesign**: Thanh điều hướng nổi Liquid Glass, thanh thẻ phiên trên đỉnh, extra-keys đồng nhất kính mờ 44dp.
- [x] **Phase 3 - Build & Sign CI (Gate 3: Passed)**: Tự động biên dịch APK release và đẩy APT metadata lên gh-pages.
- [x] **Phase F3 - Verify on Device**: Xác minh echo $PREFIX, HTTPS cert OK, tương thích AntiGravity CLI.
- [x] **Phase U2 - Polish Extra Keys & Keyboard Insets**: Đồng nhất phím tắt kính mờ, ẩn thanh điều hướng khi mở IME, dán ảnh Gboard, fix prompt bash-5.3$.
