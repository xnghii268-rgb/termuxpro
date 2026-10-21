# Termux Pro 🚀

> **Lưu ý Pháp lý & Nguồn gốc (Legal Notice & Upstream Credits)**:
> Ứng dụng **Termux Pro** là một bản phân nhánh (fork) độc lập dựa trên mã nguồn mở [termux/termux-app](https://github.com/termux/termux-app) và [termux/termux-packages](https://github.com/termux/termux-packages) theo giấy phép **GPLv3**.
> Termux Pro **hoàn toàn không phải** là ứng dụng chính thức của tổ chức Termux và không có bất kỳ liên kết chính thức nào với nhóm phát triển Termux gốc. Biểu tượng ứng dụng, mã định danh gói (`com.termuxpro.app`) và chữ ký số đều là bản phát hành hoàn toàn mới, cài đặt song song độc lập với Termux gốc.
> Toàn bộ mã nguồn được xuất bản công khai tuân thủ đầy đủ điều khoản GPLv3.

---

## 📌 1. Giới thiệu
Termux Pro là ứng dụng terminal Linux chuyên nghiệp trên nền Android với ngôn ngữ thiết kế Liquid Glass điềm tĩnh, hệ thống phím tắt extra-keys công thái học, hỗ trợ hiển thị tiếng Việt trọn vẹn và tính năng dán ảnh trực tiếp dành riêng cho các CLI AI Agents.

## 🛠️ 2. Kiến trúc & Môi trường
- **Mã gói (applicationId)**: `com.termuxpro.app`
- **Tiền tố môi trường (PREFIX)**: `/data/data/com.termuxpro.app/files/usr`
- **Thư mục Home**: `/data/data/com.termuxpro.app/files/home`
- **targetSdkVersion**: 28 (giữ nguyên để đảm bảo quyền W^X thực thi nhị phân từ thư mục dữ liệu trên Android 10+).

## 📂 3. Cấu trúc thư mục dự án
- `~/storage/downloads/termuxpro/src`: Mã nguồn chính (Source of truth).
- `~/storage/downloads/termuxpro/apk`: Nơi chứa tệp APK phát hành sau khi build hoàn tất.
- `~/storage/downloads/termuxpro/backup`: Bản sao lưu dữ liệu terminal dạng tar.gz.
- `~/.secrets/termuxpro`: Chứng chỉ ký số (`termuxpro-release.keystore`), biến môi trường bảo mật (tuyệt đối không commit vào git).
- `~/pull-termuxpro.sh`: Tập lệnh tự động tải APK phát hành mới nhất từ GitHub Actions.

## 📄 4. Giấy phép
Phát hành theo giấy phép **GNU General Public License v3.0 (GPLv3)**.
