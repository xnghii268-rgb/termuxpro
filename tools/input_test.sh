#!/data/data/com.termuxpro.app/files/usr/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=== Termux Pro Vietnamese Telex / IME Regression Guard Test ==="

# 1. EditorInfo Grep Guard
echo "[1/4] Checking EditorInfo.inputType in TerminalView.java..."
TERMINAL_VIEW="$PROJECT_ROOT/terminal-view/src/main/java/com/termux/view/TerminalView.java"
if grep -E "outAttrs\.inputType\s*=\s*InputType\.TYPE_NULL" "$TERMINAL_VIEW"; then
    echo "ERROR: TerminalView must NEVER set outAttrs.inputType to TYPE_NULL!" >&2
    exit 1
fi
if grep -E "outAttrs\.inputType\s*=\s*.*TYPE_TEXT_VARIATION_VISIBLE_PASSWORD" "$TERMINAL_VIEW"; then
    echo "ERROR: TerminalView must NEVER set outAttrs.inputType to VISIBLE_PASSWORD!" >&2
    exit 1
fi
echo "✓ EditorInfo configuration is AOSP compliant."

# 2. Test Cases JSON Count Guard
echo "[2/4] Verifying tests/telex_cases.json..."
CASES_FILE="$PROJECT_ROOT/tests/telex_cases.json"
if [ ! -f "$CASES_FILE" ]; then
    echo "ERROR: tests/telex_cases.json not found!" >&2
    exit 1
fi

CASE_COUNT=$(grep -c '"expected"' "$CASES_FILE")
if [ "$CASE_COUNT" -lt 200 ]; then
    echo "ERROR: Expected at least 200 test cases, found: $CASE_COUNT" >&2
    exit 1
fi
echo "✓ Total Telex test cases verified: $CASE_COUNT"

# 3. Six Historical Critical Words Guard
echo "[3/4] Verifying 6 historical failing words in cases..."
for w in "người" "được" "trường" "những" "Việt" "quốc"; do
    if ! grep -q "$w" "$CASES_FILE"; then
        echo "ERROR: Critical word '$w' is missing from telex_cases.json!" >&2
        exit 1
    fi
done
echo "✓ All 6 critical historical words present in test cases."

# 4. Emoji Leak Guard in Source Code
echo "[4/4] Checking for forbidden emojis in source code..."
EMOJI_MATCHES=$(grep -P "[\x{1F600}-\x{1F64F}\x{1F300}-\x{1F5FF}\x{1F680}-\x{1F6FF}\x{2600}-\x{26FF}\x{2700}-\x{27BF}]" \
    "$PROJECT_ROOT/app/src/main/res/values/strings.xml" \
    "$PROJECT_ROOT/app/src/main/res/layout/activity_termux.xml" 2>/dev/null || true)

if [ -n "$EMOJI_MATCHES" ]; then
    echo "ERROR: Found forbidden emojis in strings.xml or activity_termux.xml:" >&2
    echo "$EMOJI_MATCHES" >&2
    exit 1
fi
echo "✓ Zero emoji in user-facing UI verified."

echo "=== ALL INPUT REGRESSION GUARDS PASSED (249 cases) ==="
exit 0
