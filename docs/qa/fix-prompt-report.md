# Báo cáo kỹ thuật: Khắc phục lỗi Prompt bash-5.3$ & Tải cấu hình Shell

**Ngày thực hiện:** 21/10/2026  
**Thành phần:** `bin/bash`, `bin/login`, `TermuxInstaller.java`, `TermuxActivity.java`, `build.yml`  
**Gói ứng dụng:** `com.termuxpro.app` (Termux Pro)

---

## 1. Nguyên nhân gốc rễ (Root Cause Analysis)

Khi khởi động phiên bản mới của Termux Pro, giao diện terminal hiển thị dấu nhắc lệnh mặc định `bash-5.3$` thay vì `~ $`. Kết quả chẩn đoán chi tiết:

1. **Chuỗi đường dẫn mã hóa tĩnh trong binary `bin/bash`:**
   - Khi quét chuỗi ký tự bằng `strings bin/bash | grep -E "etc/|bashrc|profile"`, phát hiện binary bash tải về từ kho upstream vẫn chứa cứng các đường dẫn của gói cũ:
     - `/data/data/com.termux/files/usr/etc/bash.bashrc`
     - `/data/data/com.termux/files/usr/etc/profile`
     - `/data/data/com.termux/files/usr/etc/hosts`
   - Vì bash chạy trong môi trường sandbox của `com.termuxpro.app`, các đường dẫn thuộc `com.termux` không tồn tại hoặc bị chặn quyền truy cập (`EACCES` / `ENOENT`).

2. **`bin/login` vô hiệu hóa `libtermuxpro-path.so` khỏi `$LD_PRELOAD`:**
   - Trong tệp kịch bản `bin/login` (dòng 48-54), script thực hiện:
     ```bash
     export LD_PRELOAD="/data/data/com.termuxpro.app/files/usr/lib/libtermux-exec.so"
     ```
   - Thao tác này đã ghi đè hoàn toàn biến môi trường `$LD_PRELOAD`, loại bỏ thư viện đánh chặn `libtermuxpro-path.so`.
   - Do đó, khi `login` gọi `exec "$SHELL" -l "$@"`, bash thực thi mà không có bộ hook chuyển hướng đường dẫn, dẫn đến việc không thể đọc được `etc/bash.bashrc` và `etc/profile`.

3. **Thiếu tệp khung cấu hình người dùng (`~/.bashrc` và `~/.profile`):**
   - Trên thiết bị cài đặt mới, thư mục `$HOME` (`/data/data/com.termuxpro.app/files/home`) hoàn toàn trống, chưa có `.bashrc` hay `.profile`.
   - Khi cả tệp hệ thống và tệp người dùng đều không tải được, bash tự động kích hoạt prompt fallback biên dịch sẵn: `bash-5.3$`.
   - Hệ quả kéo theo: Banner `motd.sh`, các lệnh tiện ích `tp`, `menu`, `clipimg`, `lastshot` không được nạp tự động vào phiên làm việc.

---

## 2. Giải pháp kỹ thuật đã triển khai

1. **Bảo toàn `libtermuxpro-path.so` trong `bin/login`:**
   - Cập nhật quy trình build CI trong `.github/workflows/build.yml` vá `bin/login`, đảm bảo `$LD_PRELOAD` luôn giữ cả hai thư viện:
     ```bash
     export LD_PRELOAD="/data/data/com.termuxpro.app/files/usr/lib/libtermuxpro-path.so:/data/data/com.termuxpro.app/files/usr/lib/libtermux-exec.so"
     ```
   - Nhờ đó, mọi lời gọi hệ thống đọc file (`open`, `stat`, `access`) trỏ về `com.termux` từ bất kỳ binary nào đều được tự động chuyển hướng trong suốt sang `com.termuxpro.app`.

2. **Cung cấp tệp cấu hình chuẩn trong `bootstrap-overlay`:**
   - Tạo trực tiếp `app/src/main/assets/bootstrap-overlay/etc/bash.bashrc`:
     - Thiết lập `PS1` chuẩn:
       ```bash
       if [ "$TERM" = "dumb" ] || [ -n "$NO_COLOR" ]; then
           PS1='\w \$ '
       else
           PS1='\[\033[01;36m\]\w\[\033[00m\] \$ '
       fi
       ```
     - Tự động nạp các script trong `etc/profile.d/` nếu có.
   - Tạo `app/src/main/assets/bootstrap-overlay/etc/profile`:
     - Xuất các biến môi trường prefix mới `com.termuxpro.app`.
     - Tự động source `etc/bash.bashrc`.
     - Thực thi banner hacker console `etc/motd.sh`.

3. **Tự động khởi tạo cấu hình người dùng nếu chưa có (`TermuxInstaller.java`):**
   - Thêm phương thức `setupDefaultShellConfigFiles(Context)`:
     - Kiểm tra nếu `~/.bashrc` chưa có: Tạo mới với nội dung source `$PREFIX/etc/bash.bashrc`.
     - Kiểm tra nếu `~/.profile` chưa có: Tạo mới với nội dung source `$PREFIX/etc/profile` và `~/.bashrc`.
     - Tuyệt đối không ghi đè nếu người dùng đã có tệp cấu hình tùy biến.
   - Gọi hàm này ngay sau khi giải nén bootstrap và khi khởi chạy `TermuxActivity`.

4. **Khởi chạy login shell đồng nhất:**
   - Mọi phiên terminal mới đều tải đầy đủ banner, MOTD, các script tiện ích và dấu nhắc lệnh `~ $` đồng bộ.
