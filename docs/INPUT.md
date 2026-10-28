# Vietnamese Telex/VNI IME Root Cause Analysis & Architecture Contract

## 1. Executive Summary & Root Cause
Previous attempts to support Vietnamese Telex in Termux Pro attempted to intercept raw character keystrokes inside `TerminalView.onKeyDown` using a pattern-matching heuristic engine (`VietnameseTelexEngine`). This approach inevitably failed and regressed on words such as "người", "được", "trường", "những", "Việt", "quốc" due to four fundamental root causes:

### Root Cause 1: Broken EditorInfo Attributes
- In `TerminalView.onCreateInputConnection`:
  `outAttrs.inputType` was set to either `InputType.TYPE_NULL` or `InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS`.
- **Impact**: Modern Android IMEs (Gboard, OpenKey, Unikey, Laban Key) inspect `EditorInfo.inputType`. When they encounter `TYPE_NULL` or `VISIBLE_PASSWORD`, they disable their internal Vietnamese composition engine entirely, bypassing composing spans and instead emitting fragmented raw KeyEvents or unexpected commit text.
- **Fix**: Use `InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS` with `imeOptions = IME_FLAG_NO_FULLSCREEN | IME_FLAG_NO_EXTRACT_UI | IME_ACTION_NONE`. This tells the IME that standard composing is supported while preventing invasive suggestion bars.

### Root Cause 2: Missing Composing Buffer & Intermediate PTY Pollution
- Standard `BaseInputConnection` lacks a dedicated composing state buffer. When Gboard composes "ng" -> "ngu" -> "nguo" -> "nguoi" -> "người", it calls `setComposingText()` repeatedly.
- Without a composing buffer, either:
  1. Characters are immediately forwarded to the Linux PTY, which prints "nguoi", and then when the IME replaces the word it sends backspaces that desynchronize with readline/bash, leading to duplicated characters ("nguoingười" or "ngươơi").
  2. Or `setComposingText()` is discarded, dropping intermediate composition.
- **Fix**: `TerminalInputConnection` maintains an internal composing text buffer. Intermediate `setComposingText()` calls are held locally (and can be rendered with underline if desired). Only upon `commitText()` or `finishComposingText()` is the final Unicode NFC string emitted to the terminal PTY once.

### Root Cause 3: Surrounding Text & Code-Point Backspacing Mismatch
- When keyboards such as Unikey, OpenKey, or Samsung Keyboard rewrite committed text (e.g. typing "w" after "nguoi" to transform "u" and "o" into "ư" and "ơ"), they invoke `getTextBeforeCursor()` to inspect context, followed by `deleteSurroundingText(leftLength, 0)` and `commitText("người")`.
- `BaseInputConnection` was naively translating `leftLength` directly to `for (int i = 0; i < leftLength; i++) sendKeyEvent(KEYCODE_DEL)`.
- However, `leftLength` represents characters/code points in the IME's view, whereas UTF-8 multi-byte characters in terminal PTY require consistent code-point erasing. If `getTextBeforeCursor()` returned empty or inconsistent text, the IME had zero context and failed to rewrite vowels.
- **Fix**: Maintain a mirrored editable text model inside `TerminalInputConnection`. `getTextBeforeCursor()`, `getTextAfterCursor()`, and `getSelectedText()` return consistent state from the mirrored buffer. `deleteSurroundingText(before, after)` calculates exact code-point count, sends precise DEL characters to PTY, updates the mirror, and commits the replacement string.

### Root Cause 4: Unicode Normalization (NFC vs NFD)
- Some IMEs emit decomposed combining marks (NFD) such as `u` + `\u031b` (horn) or `e` + `\u0302` + `\u0301`.
- Standard terminal emulators and command-line utilities (grep, bash, git, python) expect precomposed Canonical Composition (NFC). NFD sequences in terminals cause cursor misalignment and width calculation discrepancies.
- **Fix**: All text committed to the PTY is strictly normalized via `Normalizer.normalize(text, Normalizer.Form.NFC)` before dispatch.

---

## 2. TerminalInputConnection Architecture Design

```
+-------------------------------------------------------------+
|              Android IME (Gboard / Unikey / Laban)          |
+-------------------------------------------------------------+
                             |
       setComposingText / commitText / deleteSurroundingText
                             v
+-------------------------------------------------------------+
|                   TerminalInputConnection                   |
|  - Mirrored Editable Buffer (keeps recent line context)     |
|  - Local Composing Buffer (absorbs intermediate Telex keys) |
|  - Code Point Tracker & NFC Normalizer                      |
|  - Batch Edit Coalescing (beginBatchEdit / endBatchEdit)   |
+-------------------------------------------------------------+
                             |
             Normalized NFC String / Single PTY Write
                             v
+-------------------------------------------------------------+
|               TerminalView -> TerminalEmulator              |
|               (PTY with LANG=C.UTF-8 & stty iutf8)          |
+-------------------------------------------------------------+
```

## 3. Mandatory Maintenance Contract
Any modification touching `TerminalView`, `TerminalInputConnection`, `EditorInfo`, focus management, or terminal PTY write routines **MUST**:
1. Run `./tools/input_test.sh` and ensure all 200+ cases in `tests/telex_cases.json` pass.
2. Verify that `EditorInfo.inputType` is NEVER reverted to `TYPE_NULL` or `TYPE_TEXT_VARIATION_VISIBLE_PASSWORD`.
3. Never add word-specific ad-hoc patches. Composition must remain completely generic.
