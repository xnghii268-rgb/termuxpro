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
- **Quy chuẩn Phiên bản (Version Bump Rule)**: Mỗi đợt update tính năng/giao diện mới bắt buộc nâng cả `versionCode` (+1) và `versionName` (vd: `0.120.0`, `0.121.0`...) theo chuẩn SemVer trong `app/build.gradle`.
- **CI / Kho APT**: GitHub Actions (`build.yml`) & kho APT ký GPG `https://xnghii268-rgb.github.io/termuxpro/`.

## 3. Hệ thống Giao diện Liquid Glass (Round 2 & Terminal Addendum)
- **Art Direction**: Warm graphite base (`#0E0F11`), warm cream text (`#F2EDE4`), chrome accent Terminal mint green (`#3DDC97`), champagne (`#CDB89A`). Zero red/pink/orange.
- **Living Backdrop & Overlays**: Blur 28dp, scrim 35% max, squircle 28dp. Loại bỏ 100% PreferenceFragment/AlertDialog stock.
- **Màn hình Cài đặt GlassScreen**: SettingsActivity dạng thẻ nhóm phân tầng, insets edge-to-edge trong suốt.
- **Terminal Chrome Liquid Glass**: Floating Glass Top Bar 48dp (Back chevron, Session chips cuộn ngang, "+" new session, "..." overflow), terminal view tràn viền padding 12dp hai bên, ẩn tab bar trong terminal.
- **Extra Keys Bar Liquid Glass**: 2 hàng <= 104dp, keycap squircle bo góc 10dp, nhãn sentence case (`Esc`, `Tab`, `Ctrl`, `Alt`), sticky modifier Ctrl/Alt rõ ràng.
- **MOTD Tự Động Co Giãn**: Tính toán độ rộng động theo `$COLUMNS`, đường kẻ chuẩn khớp độ rộng cột, bọc từ mẹo 2 dòng.
- **6 Bộ Theme Tiếng Việt Chuẩn**: Nguồn chân lý duy nhất (Than đêm, Mực, Rượu vang, Rêu đêm, Giấy, Sương), cursor mint `#3DDC97`, contrast >= 4.5:1.

## 4. Bộ Gõ Tiếng Việt & TerminalInputConnection
- **Tài liệu phân tích chuyên sâu**: Bắt buộc tham khảo [docs/INPUT.md](file:///data/data/com.termuxpro.app/files/home/storage/downloads/termuxpro/src/docs/INPUT.md).
- **Kiến trúc**: `TerminalInputConnection` kế thừa `BaseInputConnection`, composing buffer riêng, chuẩn hóa NFC, xóa code-point chính xác.
- **Kiểm thử tự động**: 249 cases trong `tests/telex_cases.json`, regression guard `tools/input_test.sh` đạt 100% pass.
- **Design Lint**: `tools/ui_lint.sh` kiểm soát 0 widget cũ, 0 màu lệch chuẩn, 6 theme tiếng Việt.

## 5. Trạng thái Dự án
- [x] **Phase 0 - 5**: Sửa status bar, brand redesign, GlassSheet, IME animation, tối ưu Vsync, Command Palette iOS-27.
- [x] **Phase 6 (Liquid Glass Round 1)**: Living backdrop, LiquidTabBar drop lens, Live Terminal Preview.
- [x] **Phase 7 (Liquid Glass Round 2 & Terminal Chrome)**: SettingsActivity GlassScreen, ThemePickerSheet carousel, AiAgentHubSheet, Floating Top Bar, Extra keys Liquid Glass, MOTD dynamic cols.
