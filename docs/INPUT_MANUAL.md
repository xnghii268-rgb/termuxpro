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


---

# Round 2 Manual Testing Checklist (On-Device Verification)

Run through these 8 test scenarios on a physical device:

### 1. English Commands Auto-Restore
Type each command followed by Space or Enter. Verify that words are never mangled:
- `fix` (must NOT become `fĩ`)
- `curl` (must NOT become `củl`)
- `docker` (must NOT become `dốc-...`)
- `git` (must NOT become `gịt`)
- `exit` (must NOT become `exít`)
- `test` (must NOT become `tét`)
- `raw` (must NOT become `ră`)

### 2. Compound Vowels with Trailing 'w'
- Type `thuong` then press `w` -> Expected: `thương`
- Type `truong` then press `w` then `f` -> Expected: `trường`
- Type `muon` then press `w` then `j` -> Expected: `mượn`
- Type `nguoi` then press `w` then `f` -> Expected: `người`

### 3. Shorthand Rules
- Type `duocj` -> Expected: `được`
- Type `Vietj` -> Expected: `Việt`

### 4. Double-Key Escape
- Type `aaa` -> Expected: `aa`
- Type `ww` -> Expected: `w`
- Type `ss` -> Expected: `s`
- Type `ddd` -> Expected: `dd`
- Type `ooo` -> Expected: `oo`

### 5. Fullscreen App Bypass (Alt-Screen Mode 1049)
1. Open `nano` or `vim` or `htop`.
2. Press navigation keys: `j`, `k`, `h`, `l`, `w`, `s`, `f`, `r`, `x`.
3. Verify: Cursor moves without inserting accents or modifier characters into the document.

### 6. Password Input Bypass (Echo-off)
1. Run `su` or `sudo` or SSH password prompt.
2. Type password containing `w`, `s`, `f`, `r`, `x`, `j`, `a`, `e`, `o`.
3. Verify: Password bytes are transmitted cleanly to PTY without interference or backspacing.

### 7. Quick VI / EN Extra Key Toggle
1. Tap the `VI` button in the extra keys row above the keyboard.
2. Verify: Toast displays "Bộ gõ tiếng Việt: TẮT" and button label switches to `EN`.
3. Type `thuongw` -> Output is raw `thuongw`.
4. Tap the `EN` button -> Toast displays "Bộ gõ tiếng Việt: BẬT" and button label switches to `VI`.
5. Type `thuongw` -> Output is `thương`.

### 8. Settings Customization
1. Open Settings -> **Bàn phím & Gõ tiếng Việt**.
2. Toggle "Kiểu bỏ dấu": Switch between "Hòa, Thủy (Mới)" and "Hoà, Thuỷ (Cổ điển)".
3. Test `thuyr` and `hoaf` in terminal to confirm placement changes accordingly.
