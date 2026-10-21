# Báo cáo kỹ thuật: Chuẩn hóa Extra Keys Bar & Khắc phục phím đen

**Ngày thực hiện:** 21/10/2026  
**Thành phần:** `ExtraKeysView.java`, `TermuxActivity.java`, `TermuxProThemeManager.java`, `themes.xml`  
**Gói ứng dụng:** `com.termuxpro.app` (Termux Pro)

---

## 1. Nguyên nhân gốc rễ (Root Cause Analysis)

Qua điều tra chi tiết mã nguồn và layout phân cấp, nguyên nhân gây ra hiện tượng phím đen và hai kiểu dáng phím lẫn lộn gồm:

1. **Thuộc tính màu nền chủ đề (`themes.xml`):**
   - Trong `res/values/themes.xml` và `res/values-night/themes.xml`, thuộc tính `extraKeysButtonBackgroundColor` được gán cố định là `@color/black` (`#000000`).
   - `extraKeysButtonActiveBackgroundColor` được gán cố định là `@color/grey_500`.

2. **Cơ chế xử lý chạm trong `ExtraKeysView.java`:**
   - Khi khởi tạo, các phím được tạo bằng `new MaterialButton(..., buttonBarButtonStyle)` với nền mặc định trong suốt.
   - Tuy nhiên, trong `setOnTouchListener`:
     - Khi nhấn xuống (`ACTION_DOWN`), gọi `view.setBackgroundColor(mButtonActiveBackgroundColor)`.
     - Khi nhấc ngón tay (`ACTION_UP`) hoặc hủy (`ACTION_CANCEL`), gọi `view.setBackgroundColor(mButtonBackgroundColor)` (tức là màu đen thuần `#000000`).
   - Lệnh `setBackgroundColor` đã ghi đè toàn bộ drawable nền của `MaterialButton`, biến các phím đã nhấn (hoặc phím có bộ lặp như `ESC`, `↑`, `←`, `↓`, `→`, `/`) thành các khối màu đen tuyền.
   - Các phím chưa được chạm vào hoặc các phím đặc biệt (`CTRL`, `ALT`) vốn chỉ đổi màu chữ (`setTextColor`) thì giữ nguyên nền trong suốt, tạo ra hai giao diện phím đối lập nhau hoàn toàn.

3. **Khoảng cách giữa các phím bằng 0:**
   - Trong `GridLayout.LayoutParams`, giá trị lề được đặt cứng là `param.setMargins(0, 0, 0, 0)`.
   - Khi các phím chuyển sang màu đen, chúng dính chặt vào nhau tạo thành một dải đen liền khối không có rãnh ngăn cách.

4. **Tràn dòng chữ nhãn ("DÁN ẢNH", "ẢNH MỚI"):**
   - Chữ viết hoa toàn bộ kết hợp với khoảng cách ký tự (`letterSpacing`) mặc định rộng vượt quá chiều rộng của một ô phím (~46dp trên màn hình 360dp), dẫn đến bị ngắt thành hai dòng.

---

## 2. Giải pháp kỹ thuật đã triển khai (Unified Glass Key Design)

1. **Đồng nhất 1 Style duy nhất cho toàn bộ các phím:**
   - Hình khối: Hình chữ nhật bo góc `10dp` (`GradientDrawable.RECTANGLE`, `cornerRadius = 10dp`).
   - Viền hairline tinh tế `0.5dp` (`stroke = 0.5dp`).
   - Khoảng cách phím: Đặt margin mỗi bên `2dp` (`param.setMargins(marginPx, marginPx, marginPx, marginPx)`), tạo rãnh ngăn cách chuẩn xác `4dp` giữa các phím liền kề.
   - Màu nền dựa trên token ngữ cảnh (Semantic Tokens):
     - Chủ đề tối (Dark Theme): Bar nền = `terminalBg + 8% white` (alpha `0xEE`), phím thường = `terminalBg + 12% white`, viền `0x26FFFFFF` (15% white).
     - Chủ đề sáng (Light Theme): Bar nền = `terminalBg + 8% black` (alpha `0xEE`), phím thường = `terminalBg + 6% black`, viền `0x1F000000` (12% black).
     - Tuyệt đối không dùng màu đen cứng `#000000`. Đảm bảo độ tương phản chữ và nền luôn >= 4.5:1 trên cả 6 chủ đề giao diện.

2. **Hiệu ứng nhấn (Pressed State) & Phím dính (Active Toggles):**
   - Khi nhấn: Phím thu nhỏ mượt mà theo tỷ lệ `scale 0.97` (thời lượng 150ms) và tăng độ sáng nhẹ (`terminalBg + 22% white` hoặc `15% black`), không chuyển sang màu đen.
   - Khi nhả: Tự động hồi phục `scale 1.0` (150ms) và trở về nền kính chuẩn.
   - Phím dính (`Ctrl`, `Alt` khi kích hoạt): Nền chuyển sang màu nhấn bán trong suốt (`accentBgColor`), viền và chữ chuyển màu nhấn sáng (`accentColor`), hiển thị trạng thái đang bật rõ ràng.
   - Phím tác vụ nổi bật (`Dán ảnh`, `Ảnh mới`, `Lệnh`): Áp dụng tông màu nhấn accent dịu mắt.

3. **Khắc phục triệt để phím bị kẹt/đen khi đổi xoay hoặc chuyển ứng dụng:**
   - Bổ sung hàm `resetExtraKeysState()` hủy bỏ toàn bộ executor lặp, đưa tỷ lệ phím về `1.0f` và khôi phục nền chuẩn.
   - Tự động gọi `resetExtraKeysState()` trong `onPause()` và `onResume()` của `TermuxActivity`.

4. **Chuẩn hóa nhãn phím (Sentence case, không tràn dòng):**
   - Chuyển toàn bộ nhãn sang sentence case: `"Esc"`, `"Ctrl"`, `"Alt"`, `"Home"`, `"End"`, `"Dán ảnh"`, `"Ảnh mới"`, `"Lệnh"`.
   - Sử dụng biểu tượng chuẩn cho phím điều hướng và Tab: `"⇥"`, `"↑"`, `"←"`, `"↓"`, `"→"`.
   - `button.setAllCaps(false)`, `button.setLetterSpacing(0f)`, `button.setMaxLines(1)`, `button.setSingleLine(true)`.
   - Sử dụng `TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(button, 10, 14, 1, TypedValue.COMPLEX_UNIT_SP)` tự động co giãn font chữ trong khoảng 10sp–14sp, đảm bảo nhãn hiển thị trọn vẹn trên 1 dòng tại các độ rộng màn hình 360px, 390px, 412px.
   - Chiều cao vùng chạm tối thiểu luôn đạt `>= 44dp`.
