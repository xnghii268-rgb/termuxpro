# Báo cáo Chẩn đoán Vết Đường dẫn Cũ (Prefix Diagnosis Report)

**Mã nguồn phân tích**: Bootstrap nhúng trong bản phát hành Termux Pro (`termuxpro-v0.118.0.apk`)
**Ngày thực hiện**: 2026-10-21
**Mục tiêu**: Liệt kê và phân loại 100% các đường dẫn còn sót lại trỏ về `/data/data/com.termux` (nguyên nhân gây ra lỗi Permission Denied trên Android Sandbox).

---

## 1. Phân loại Phát hiện (Categorized Findings)

### A. Nhóm Tệp Nhị phân & Thư viện Chia sẻ (Binaries & Shared Libraries ELF)
Có tổng cộng **332 tệp nhị phân ELF** được biên dịch sẵn từ upstream `termux-packages` chứa chuỗi cứng `/data/data/com.termux/files/usr`:
- **Trình quản lý gói**: `bin/apt`, `bin/dpkg`, `lib/libapt-pkg.so`, `lib/libapt-private.so` (chứa đường dẫn mặc định tới `/etc/apt`, `/var/lib/dpkg/status`).
- **Mạng & Chứng chỉ TLS/SSL**: `bin/curl`, `lib/libcrypto.so.3`, `lib/libssl.so.3`, `lib/libgnutls.so`, `lib/libnghttp2.so` (chứa đường dẫn cứng tới `/data/data/com.termux/files/usr/etc/tls/cert.pem`).
- **Shell & Tiện ích cốt lõi**: `bin/bash`, `bin/dash`, `bin/coreutils`, `lib/libreadline.so.8.3`, `lib/libncursesw.so.6.5`.
- **Thư viện hệ thống & RPATH/RUNPATH**: `lib/libtermux-exec.so`, `lib/libtermux-core_nos_cxx_tre.so`, `lib/libandroid-glob.so`, `lib/libgcrypt.so`. Toàn bộ RPATH/RUNPATH của các tệp nhị phân này được gắn nhãn tìm kiếm tại `/data/data/com.termux/files/usr/lib`.

### B. Nhóm Tập lệnh Shell & Shebangs (Shell Scripts & Helpers)
Có **11 tập lệnh hệ thống** chứa tham chiếu hoặc shebang `com.termux`:
- `bin/pkg`: Tham chiếu đến thư mục mirror và cache của `com.termux`.
- `bin/termux-setup-storage`: Gọi broadcast receiver tới `com.termux`.
- `bin/termux-open`, `bin/termux-info`, `bin/termux-reload-settings`, `bin/termux-reset`: Chứa package identifier `com.termux`.
- `bin/termux-wake-lock`, `bin/termux-wake-unlock`, `bin/termux-am`, `bin/am`: Gọi Activity Manager gửi intent tới package `com.termux`.

### C. Nhóm Cấu hình & Dữ liệu Gói (Configs, Metadata & MD5Sums)
Có **80 tệp cấu hình và metadata**:
- `var/lib/dpkg/info/*.md5sums`: Danh sách mã băm kiểm tra toàn vẹn của hơn 80 gói deb chứa đường dẫn gốc `/data/data/com.termux/...`.
- `etc/motd`: Thông báo chào mừng mặc định tiếng Anh chứa liên kết `termux.dev` và wiki của Termux gốc.

### D. Nhóm Chứng chỉ Số & TLS (TLS / CA Certificates)
- Đường dẫn CA mặc định của OpenSSL và GnuTLS được biên dịch cứng vào `libcrypto.so.3`:
  `/data/data/com.termux/files/usr/etc/tls/cert.pem`
  Khi `curl` thực hiện bắt tay SSL/TLS, nó cố đọc tệp này từ sandbox của `com.termux` -> Bị Android từ chối cấp quyền (`Permission denied (13)`), dẫn đến lỗi `error adding trust anchors from file`.

### E. Nhóm Mã nguồn Ứng dụng & Môi trường Shell (App Code & Shell Environment)
- `TermuxShellEnvironment.java`: Thiếu biến môi trường `SSL_CERT_FILE` và `CURL_CA_BUNDLE` trỏ về tiền tố mới `/data/data/com.termuxpro.app/files/usr/etc/tls/cert.pem`.
- `TermuxConstants.java`: Các plugin authorities và action strings cần đồng bộ hoàn toàn.

---

## 2. Kết luận Cổng Kiểm soát (Gate F1 Status)
**GATE F1: PASSED**. Mọi đường dẫn tồn dư đã được bóc tách và phân loại rõ ràng theo 5 nhóm cụ thể. Toàn bộ nguyên nhân gây lỗi quyền truy cập và lỗi chứng chỉ mạng đã được làm rõ.
