<p align="center">
  <img src="fastlane/metadata/android/en-US/images/icon.png" width="128" height="128" alt="Termux Pro Icon" />
</p>

<h1 align="center">Termux Pro</h1>

<p align="center">
  <strong>The Next-Generation Linux Terminal for Android & AI Agents.</strong><br>
  Forged with Liquid Glass aesthetics, zero-compromise Vietnamese IME, and first-class AI agent workflows.
</p>

<p align="center">
  <a href="https://github.com/xnghii268-rgb/termuxpro/releases/latest"><img src="https://img.shields.io/github/v/release/xnghii268-rgb/termuxpro?style=flat-square&color=3DDC97&label=Release" alt="Latest Release"></a>
  <a href="LICENSE.md"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg?style=flat-square" alt="License: GPLv3"></a>
  <img src="https://img.shields.io/badge/Platform-Android%207.0%2B-brightgreen.svg?style=flat-square" alt="Platform: Android 7.0+">
  <img src="https://img.shields.io/badge/targetSdkVersion-28%20(W%5EX%20Exec)-orange.svg?style=flat-square" alt="targetSdkVersion 28">
  <img src="https://img.shields.io/badge/Architecture-arm64--v8a-lightgrey.svg?style=flat-square" alt="Arch: arm64-v8a">
  <img src="https://img.shields.io/badge/Tests-16%2C078%20Passed-success.svg?style=flat-square" alt="Tests Passed">
</p>

---

## ⚡ Tải xuống ngay (Download APK)

| Phiên bản | Tệp phát hành | Trạng thái | SHA-256 Checksum |
| :--- | :--- | :--- | :--- |
| **v0.120.14** (Mới nhất) | [**Tải Termux Pro APK**](https://github.com/xnghii268-rgb/termuxpro/releases/download/v0.120.14/termuxpro-v0.120.14.apk) | 🟢 Ổn định (Release) | `eda771d40b149f84b2a9ad450eb5ec0675a713bd435dc3e74994bef09b6ac3c4` |

> 💡 **Cài đặt song song 100%**: Mã định danh gói là `com.termuxpro.app`, cài đặt độc lập hoàn toàn với Termux gốc mà không gây xung đột hay đè dữ liệu cũ.

---

## 💎 Điểm đột phá (Key Highlights)

### 1. 🧊 Ngôn ngữ thiết kế Liquid Glass (iOS 26 Style)
- **Living Backdrop & Floating Chrome**: Thanh tiêu đề kính nổi 48dp bo cong mềm mại, tích hợp hiển thị phiên làm việc (Session Chips) và chuyển đổi tác vụ tức thì.
- **Bàn phím Extra-Keys công thái học**: Phím squircle bo góc 10dp, phản hồi xúc giác (Haptic Feedback), sticky modifier (`Ctrl`, `Alt`) trực quan không lo kẹt phím.
- **6 Bộ Palette màu chuyên nghiệp**: Than đêm, Mực, Rượu vang, Rêu đêm, Giấy và Sương. Màu con trỏ ngọc lục bảo (`#3DDC97`) với độ tương phản chuẩn WCAG 4.5:1.

### 2. 🇻🇳 Đột phá bộ gõ Tiếng Việt: Single-Writer Pipeline v3
- **Không còn ác mộng nuốt phím / dính dấu**: Vượt qua 16,078 kịch bản kiểm thử tự động, loại bỏ hoàn toàn các lỗi kinh điển trên terminal Android (mất chữ 'd', lặp thanh dấu khi gõ `ter/pass/word`, crash bộ đệm Gboard/LabanKey).
- **Bộ nhớ ngữ âm thuần Java (Zero-Dependency JVM)**: `VietnameseEngine` độc lập, phản hồi độ trễ cực thấp (< 16ms).
- **Bảo vệ lệnh Linux Tiếng Anh**: Tự động nhận diện lệnh bash (`curl`, `git`, `docker`, `find`, `wget`, `exit`) tránh méo ký tự ngoài ý muốn.
- **Smart Bypass**: Tự động vô hiệu hóa thông minh trong màn hình TUI/Alt-Screen (`vim`, `nano`, `htop`, `tmux`) và khi nhập mật khẩu ẩn (`isPtyEcho == false`).
- **Phím chuyển tức thời**: Nút `VI`/`EN` đặt ngay trên thanh extra-keys để đổi chế độ chỉ với 1 chạm.

### 3. 🤖 Tối ưu tuyệt đối cho CLI AI Agents
- **Photo Picker & Dán ảnh vào Terminal**: Nhấn nút `Ảnh` để mở thư viện ảnh hệ thống, chọn tối đa 50 hình ảnh cùng lúc để dán trực tiếp đường dẫn / dữ liệu hình ảnh vào các trợ lý dòng lệnh như Antigravity (AGY), Claude Code, Gemini CLI, Aider.
- **AI Agent Hub**: Tích hợp sẵn bảng điều khiển phím tắt và menu hỗ trợ các phiên làm việc lập trình tự hành dài hạn.

### 4. 🚀 Tự do thực thi nhị phân (Unrestricted Execution)
- Duy trì chuẩn `targetSdkVersion 28` giúp nhà phát triển toàn quyền thực thi nhị phân động (W^X) trực tiếp trong thư mục `$PREFIX/bin` mà không bị chính sách SELinux khắt khe của Android 10+ chặn đứng.

---

## 📊 Bảng so sánh tính năng (Comparison)

| Tính năng | Termux Gốc (Upstream) | Termux Pro 🚀 |
| :--- | :---: | :---: |
| **Giao diện người dùng** | Android Holo / Material cổ điển | **Liquid Glass (Translucent, Frosted Sheets)** |
| **Gõ Tiếng Việt (Telex/VNI)** | Thường xuyên nuốt ký tự, kẹt dấu | **Single-Writer Engine v3 (16k+ Test Passed)** |
| **Khôi phục lệnh Tiếng Anh** | Thủ công xóa gõ lại | **Tự động nhận diện lệnh CLI tiếng Anh** |
| **Dán ảnh & Thư viện ảnh** | Không hỗ trợ trực tiếp | **Photo Picker đa ảnh + Dán ảnh Clipboard** |
| **Cài đặt song song** | Gốc (`com.termux`) | **Độc lập hoàn toàn (`com.termuxpro.app`)** |
| **Thanh phím Extra-Keys** | Cơ bản, dễ kẹt modifier | **Squircle công thái học, haptic feedback** |
| **Chữ ký số phát hành** | Thay đổi tùy nguồn tải | **Chữ ký release v2/v3 bảo đảm cài đè 100%** |

---

## 🛠️ Cấu trúc Môi trường & Hệ thống

| Thành phần | Đường dẫn / Giá trị |
| :--- | :--- |
| **Application ID** | `com.termuxpro.app` |
| **PREFIX (Root Linux)** | `/data/data/com.termuxpro.app/files/usr` |
| **HOME Directory** | `/data/data/com.termuxpro.app/files/home` |
| **Kiến trúc khuyến nghị** | `arm64-v8a` |
| **Nền tảng Android** | Android 7.0 (API 24) đến Android 15+ |

---

## 💻 Hướng dẫn Biên dịch từ Mã nguồn (Build from Source)

### Yêu cầu tiên quyết:
- JDK 17
- Android SDK (với build-tools & platform-tools tương thích)
- Git

### Các bước thực hiện:

```bash
# 1. Clone mã nguồn
git clone https://github.com/xnghii268-rgb/termuxpro.git
cd termuxpro

# 2. Cấp quyền thực thi cho gradle wrapper
chmod +x gradlew

# 3. Chạy kiểm thử bộ gõ Single-Writer Pipeline
./tools/input_test.sh

# 4. Biên dịch APK bản Release
./gradlew assembleRelease
```

File APK xuất xưởng sẽ nằm tại:
`app/build/outputs/apk/release/termuxpro-release.apk`

---

## ⚖️ Tuyên bố Pháp lý & Ghi nhận Nguồn gốc (Legal & Upstream Notice)

- **Bản quyền mã nguồn**: Dự án **Termux Pro** được phát triển dưới hình thức phân nhánh độc lập (fork) tuân thủ nghiêm ngặt giấy phép mã nguồn mở **GNU General Public License v3.0 (GPLv3)**.
- **Ghi nhận tổ chức gốc**: Termux Pro ghi nhận và tri ân sâu sắc công sức của đội ngũ phát triển [Termux](https://github.com/termux) đối với các dự án nền tảng `termux-app` và `termux-packages`.
- Termux Pro hoàn toàn độc lập, không phải là sản phẩm chính thức và không có quan hệ bảo trợ từ tổ chức Termux.

---

## 📄 Giấy phép (License)

Toàn bộ dự án được phát hành theo giấy phép **[GNU General Public License v3.0 (GPLv3)](LICENSE.md)**.
Mọi cải tiến và mã nguồn đóng góp đều tuân theo nguyên tắc tự do của phần mềm nguồn mở.
