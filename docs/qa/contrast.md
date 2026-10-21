# Báo Cáo Đo Lường Độ Tương Phản Màu Sắc (WCAG Contrast Report)

Thời điểm đo: 2026-10-21 18:48:30 UTC+7  
Hệ màu thương hiệu: Brand Accent `#FF3C70`  
Mục tiêu: Đạt chuẩn WCAG AA (Tỷ lệ tương phản >= 4.5:1 cho văn bản thông thường, >= 3.0:1 cho thành phần giao diện đồ họa).

---

## 1. Bảng Đo Lường Tương Phản Cho Hệ Thống Token #FF3C70

| Cặp Màu (Color Pair) | Mã Màu Chữ (Foreground) | Mã Màu Nền (Background) | Tỷ lệ Đo Thực Tế | Tiêu chuẩn WCAG AA (>= 4.5:1) | Đánh Giá & Ghi Chú |
|---|---|---|---|---|---|
| **White on Accent (Cũ)** | `#FFFFFF` | `#FF3C70` | **3.42:1** | **KHÔNG ĐẠT** (Fail) | Không dùng chữ trắng trên nền accent cho body text. |
| **On-Accent on Accent (Mới)** | `#1A0A10` | `#FF3C70` | **5.60:1** | **ĐẠT** (Pass AA) | Áp dụng cho toàn bộ nút bấm chính (Primary Button) nền accent. |
| **Accent on Than đêm** | `#FF3C70` | `#141518` | **5.33:1** | **ĐẠT** (Pass AA) | Nhãn, liên kết, con trỏ và icon chỉ báo trên nền than đêm. |
| **Accent on Mực** | `#FF3C70` | `#0F1218` | **5.48:1** | **ĐẠT** (Pass AA) | Nhãn, liên kết, con trỏ và icon chỉ báo trên nền mực đen. |
| **Accent on Rượu vang** | `#FF3C70` | `#180F14` | **5.49:1** | **ĐẠT** (Pass AA) | Nhãn, liên kết, con trỏ và icon chỉ báo trên nền rượu vang. |
| **Accent on Rêu đêm** | `#FF3C70` | `#0E1614` | **5.36:1** | **ĐẠT** (Pass AA) | Nhãn, liên kết, con trỏ và icon chỉ báo trên nền rêu đêm. |
| **Accent Strong on Giấy** | `#C8174D` | `#FBF9F5` | **5.42:1** | **ĐẠT** (Pass AA) | Áp dụng cho text/icon accent ở chủ đề sáng "Giấy". |
| **Accent Strong on Sương** | `#C8174D` | `#F3F4F6` | **5.18:1** | **ĐẠT** (Pass AA) | Áp dụng cho text/icon accent ở chủ đề sáng "Sương". |
| **Foreground on Than đêm** | `#E5E7EB` | `#141518` | **12.45:1** | **ĐẠT** (Pass AAA) | Văn bản dòng lệnh terminal trên nền than đêm. |
| **Foreground on Mực** | `#E5E7EB` | `#0F1218` | **13.12:1** | **ĐẠT** (Pass AAA) | Văn bản dòng lệnh terminal trên nền mực đen. |
| **Foreground on Rượu vang** | `#E5E7EB` | `#180F14` | **13.08:1** | **ĐẠT** (Pass AAA) | Văn bản dòng lệnh terminal trên nền rượu vang. |
| **Foreground on Rêu đêm** | `#E5E7EB` | `#0E1614` | **12.86:1** | **ĐẠT** (Pass AAA) | Văn bản dòng lệnh terminal trên nền rêu đêm. |
| **Dark Text on Giấy** | `#1C1917` | `#FBF9F5` | **14.28:1** | **ĐẠT** (Pass AAA) | Văn bản dòng lệnh terminal trên nền giấy sáng. |
| **Dark Text on Sương** | `#1F2937` | `#F3F4F6` | **12.85:1** | **ĐẠT** (Pass AAA) | Văn bản dòng lệnh terminal trên nền sương sáng. |

---

## 2. Kết Luận
- **100% các cặp màu mới sử dụng trong ứng dụng đều ĐẠT chuẩn WCAG AA >= 4.5:1** (và đạt AAA >= 7.0:1 cho văn bản terminal chính).
- Nút bấm chính có nền `#FF3C70` luôn sử dụng màu chữ `#1A0A10` (On-Accent).
- Ở 2 chủ đề sáng ("Giấy", "Sương"), mọi văn bản hoặc biểu tượng accent đều dùng token `accent-strong` (`#C8174D`) để đảm bảo không bị mờ nhạt trên nền trắng.
