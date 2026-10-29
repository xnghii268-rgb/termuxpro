# Real Keyboard Traces (Golden Test Set)

This directory contains real keyboard IME traces recorded on actual Android devices (Gboard Vietnamese Telex, Gboard English, Samsung Keyboard, Laban Key, and Termux Pro Built-in Keyboard).

Traces can be recorded directly from the app via:
**Settings > Bàn phím & Nhập liệu > Tự kiểm tra bộ gõ > Bắt đầu ghi vết (Start Record) > Dừng & Xuất vết JSON**.

The exported trace JSON files are stored in `/sdcard/Download/termux-pro-ime-trace-<timestamp>.json` and replayed in the automated test runner to guarantee zero regressions against physical device behaviors.
