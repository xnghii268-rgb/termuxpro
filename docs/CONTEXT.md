# Termux Pro Context & Architecture

Termux Pro là ứng dụng terminal Linux độc lập phân nhánh từ termux-app và termux-packages (GPLv3).

## 1. Bố cục Thư mục (Target Folder Layout)
- `~/storage/downloads/termuxpro/src`: Mã nguồn chính (Git repo xnghii268-rgb/termuxpro).
- `~/storage/downloads/termuxpro/apk`: Lưu trữ APK phát hành (`termuxpro-v0.120.0.apk`).
- `~/storage/downloads/termuxpro/backup`: Lưu trữ bản sao lưu `.tar.gz` của home.
- `~/.secrets/termuxpro`: Keystore phát hành và GPG key signing (`termuxpro-apt-private.asc`, chmod 600).
- `~/pull-termuxpro.sh`: Tập lệnh tự động kéo APK phát hành mới nhất từ GitHub Actions qua `gh`.

## 2. Thông số Kỹ thuật & Môi trường
- **Mã định danh gói (applicationId)**: `com.termuxpro.app` (cài song song độc lập với Termux gốc).
- **Tiền tố Linux (PREFIX)**: `/data/data/com.termuxpro.app/files/usr` (Home: `/data/data/com.termuxpro.app/files/home`).
- **targetSdkVersion**: 28 | **Kiến trúc**: `arm64-v8a` | **Phiên bản**: v0.120.1 (code 134).
- **Quy chuẩn Phiên bản**: Mỗi đợt update tính năng/giao diện mới bắt buộc nâng cả `versionCode` (+1) và `versionName` (vd: `0.120.0`, `0.121.0`...) theo chuẩn SemVer trong `app/build.gradle`.
- **CI / Kho APT**: GitHub Actions (`build.yml`) & kho APT ký GPG `https://xnghii268-rgb.github.io/termuxpro/`.

## 3. Hệ thống Giao diện Liquid Glass (Round 2 & Terminal Addendum)
- **Art Direction**: Warm graphite base (`#0E0F11`), warm cream text (`#F2EDE4`), chrome accent Terminal mint green (`#3DDC97`), champagne (`#CDB89A`). Zero red/pink/orange.
- **Living Backdrop & Overlays**: Blur 28dp, scrim 35% max, squircle 28dp. Loại bỏ 100% PreferenceFragment/AlertDialog stock.
- **Màn hình Cài đặt GlassScreen**: SettingsActivity dạng thẻ nhóm phân tầng, insets edge-to-edge trong suốt.
- **Terminal Chrome Liquid Glass**: Floating Glass Top Bar 48dp (Back chevron, Session chips cuộn ngang, "+" new session, "..." overflow), terminal view tràn viền padding 12dp hai bên, ẩn tab bar trong terminal.
- **Extra Keys Bar Liquid Glass**: 2 hàng <= 104dp, keycap squircle bo góc 10dp, nhãn sentence case (`Esc`, `Tab`, `Ctrl`, `Alt`), sticky modifier Ctrl/Alt rõ ràng.
- **MOTD Tự Động Co Giãn**: Tính toán độ rộng động theo `$COLUMNS`, đường kẻ chuẩn khớp độ rộng cột, bọc từ mẹo 2 dòng.
- **6 Bộ Theme Tiếng Việt Chuẩn**: Nguồn chân lý duy nhất (Than đêm, Mực, Rượu vang, Rêu đêm, Giấy, Sương), cursor mint `#3DDC97`, contrast >= 4.5:1.

## 4. Bộ Gõ Tiếng Việt & TerminalInputConnection (Round 2)
- **Tài liệu phân tích chuyên sâu**: Tham khảo [docs/INPUT.md](file:///data/data/com.termuxpro.app/files/home/storage/downloads/termuxpro/src/docs/INPUT.md) & [docs/INPUT_MANUAL.md](file:///data/data/com.termuxpro.app/files/home/storage/downloads/termuxpro/src/docs/INPUT_MANUAL.md).
- **Kiến trúc Pure Java JVM**: `VietnameseEngine` và `VietnamesePhonotactics` thuần JVM 0 dependency Android, hàm không trạng thái `f(rawKeys, options)`, validate âm tiết ngữ âm chuẩn xác.
- **Khôi phục lệnh Tiếng Anh tự động**: Lệnh Linux (`fix`, `docker`, `curl`, `git`, `exit`, `test`, `raw`) không bao giờ bị méo dấu.
- **Bypass thông minh**: Tự tắt trong alt-screen (vim/nano/htop) & khi nhập mật khẩu ẩn (`isPtyEcho() == false`).
- **Nút bật/tắt nhanh**: Nút `VI`/`EN` trên thanh extra keys chuyển đổi bộ gõ tức thì.
- **Kiểm thử tự động**: 4940 assertions đạt 100% pass qua `tools/input_test.sh` (1032 engine cases, 418 English words, 3200 Vietnamese words, 249 legacy cases).

## 5. Trạng thái Dự án
- [x] **Phase 0 - 7**: Brand redesign, Liquid Glass system, GlassScreen, Floating Top Bar, 6 Vietnamese themes.
- [x] **Phase 8 (Vietnamese Input Round 2)**: Pure JVM engine, English auto-restore, alt-screen/password bypass.
- [x] **Phase 9 (Single-Writer Pipeline v3)**: InputModel mirror, RegionWriter PTY sole-writer, InputProducer RAW vs COMPOSER, latency <= 16ms, repeat-key undo table N=1..12.
- [x] **Phase 10 (Real-Keyboard Self-Test & Built-in Keyboard)**: ImeSelfTestActivity 14-phrases, live HUD, JSON trace export, BuiltinKeyboardView glass layout. 6877/6877 test assertions pass 100%.
- [x] **Phase 11 (Input v3 Hotfix: Non-Disappearing Letters)**: Fix commitText non-composing branch, InputModel span shrink, Rule 1 committed text protection, Safe Mode & Auto-Fallback Toast, Full-Line Oracle Fuzz 10k ops. 15,206 assertions pass 100%.
- [x] **Phase 12 (Input v3 Hotfix 2: Vietnamese-Leaning Auto-Restore)**: Fix parseBody null-fallback returning raw buffer (ddc -> ddc), Vietnamese-leaning policy (lexicon >= 20k, foreign initials, command position, explicit transforms win), manual VI/EN override per word, 3-state setting (OFF/SMART/AGGRESSIVE). 16,078 assertions pass 100%.
