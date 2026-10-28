# Manual Verification Guide for Vietnamese IME (Gboard, Unikey, OpenKey)

`adb shell input text` does NOT exercise the Android InputConnection composing spans, so manual verification on a real touchscreen keyboard is required for full validation.

## 20 Reference Words Checklist
Open Termux Pro, tap into the Terminal prompt, and type each word followed by ENTER:

1. `echo "người"` (Key sequence: `nguowfi` + Space) -> Expected: `người`
2. `echo "được"` (Key sequence: `dduwowcj` + Space) -> Expected: `được`
3. `echo "trường"` (Key sequence: `truwowngf` + Space) -> Expected: `trường`
4. `echo "những"` (Key sequence: `nhuwngx` + Space) -> Expected: `những`
5. `echo "Việt"` (Key sequence: `Vieetj` + Space) -> Expected: `Việt`
6. `echo "quốc"` (Key sequence: `quoocs` + Space) -> Expected: `quốc`
7. `echo "thương"` (Key sequence: `thuowng` + Space) -> Expected: `thương`
8. `echo "nguyễn"` (Key sequence: `nguyeexn` + Space) -> Expected: `nguyễn`
9. `echo "truyền"` (Key sequence: `truyeenf` + Space) -> Expected: `truyền`
10. `echo "hoàng"` (Key sequence: `hoangf` + Space) -> Expected: `hoàng`
11. `echo "khuyến"` (Key sequence: `khuyeens` + Space) -> Expected: `khuyến`
12. `echo "tuyệt"` (Key sequence: `tuyeetj` + Space) -> Expected: `tuyệt`
13. `echo "nghệ"` (Key sequence: `ngheej` + Space) -> Expected: `nghệ`
14. `echo "ngô"` (Key sequence: `ngoo` + Space) -> Expected: `ngô`
15. `echo "ghế"` (Key sequence: `ghees` + Space) -> Expected: `ghế`
16. `echo "hươu"` (Key sequence: `huowu` + Space) -> Expected: `hươu`
17. `echo "hoà"` (Key sequence: `hoaf` + Space) -> Expected: `hoà`
18. `echo "thuỳ"` (Key sequence: `thuyf` + Space) -> Expected: `thuỳ`
19. `echo "Ắ Ử Ỡ Đ Ơ Ư"` -> Expected: `Ắ Ử Ỡ Đ Ơ Ư`
20. `echo "aa aaa"` (Double-key escape) -> Expected: `aa aaa`

## Byte Verification
Run in terminal to confirm single-cell NFC representation (no stray NFD combining marks):
```bash
printf '%s' "người được trường" | od -c
```
Expected output:
- Each Vietnamese character should be encoded in valid precomposed UTF-8 bytes without orphan combining code points.
