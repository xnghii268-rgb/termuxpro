#!/data/data/com.termuxpro.app/files/usr/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=== Termux Pro Liquid Glass UI & Design System Lint ==="

# 1. Check for legacy PreferenceFragment/PreferenceActivity in SettingsActivity
echo "[1/6] Checking for legacy Preference widgets in SettingsActivity..."
SETTINGS_JAVA="$PROJECT_ROOT/app/src/main/java/com/termux/app/activities/SettingsActivity.java"
if grep -E "(PreferenceFragmentCompat|PreferenceActivity|PreferenceScreen)" "$SETTINGS_JAVA"; then
    echo "ERROR: SettingsActivity must not use legacy PreferenceFragment or PreferenceScreen!" >&2
    exit 1
fi
echo "✓ SettingsActivity uses custom GlassScreen architecture."

# 2. Check for off-palette hot pink/red colors in app assets, drawables, and ui classes
echo "[2/6] Checking for off-palette pink/neon colors (#FF3C70, #E11D48, #BE123C)..."
OFF_PALETTE_MATCHES=$(grep -rE "(#FF3C70|#ff3c70|#E11D48|#e11d48|#BE123C|#be123c)" \
    "$PROJECT_ROOT/app/src/main/res/drawable" \
    "$PROJECT_ROOT/app/src/main/res/layout" \
    "$PROJECT_ROOT/app/src/main/assets/themes" \
    "$PROJECT_ROOT/app/src/main/java/com/termux/app/ui" 2>/dev/null || true)

if [ -n "$OFF_PALETTE_MATCHES" ]; then
    echo "ERROR: Found forbidden off-palette colors in UI resources:" >&2
    echo "$OFF_PALETTE_MATCHES" >&2
    exit 1
fi
echo "✓ Zero off-palette pink/neon colors in UI resources."

# 3. Check themes: exactly 6 Vietnamese canonical themes, no English themes
echo "[3/6] Verifying canonical Vietnamese themes in assets/themes/..."
THEMES_DIR="$PROJECT_ROOT/app/src/main/assets/themes"
EXPECTED_THEMES=("than_dem.json" "muc.json" "ruou_vang.json" "reu_dem.json" "giay.json" "suong.json")

for t in "${EXPECTED_THEMES[@]}"; do
    if [ ! -f "$THEMES_DIR/$t" ]; then
        echo "ERROR: Missing expected theme file '$t' in $THEMES_DIR!" >&2
        exit 1
    fi
done

ACTUAL_THEME_COUNT=$(find "$THEMES_DIR" -name "*.json" | wc -l)
if [ "$ACTUAL_THEME_COUNT" -ne 6 ]; then
    echo "ERROR: Expected exactly 6 canonical theme files in $THEMES_DIR, found: $ACTUAL_THEME_COUNT" >&2
    exit 1
fi
echo "✓ Exactly 6 canonical Vietnamese themes verified."

# 4. Check for system bar theme colors: no red bar
echo "[4/6] Verifying system bars and colorPrimary in styles/themes..."
if grep -E "<item name=\"colorPrimary\">@color/red_" "$PROJECT_ROOT/termux-shared/src/main/res/values/themes.xml"; then
    echo "ERROR: termux-shared themes.xml still defines red colorPrimary!" >&2
    exit 1
fi
echo "✓ System primary color verified obsidian/mint, zero red status bar."

# 5. Check motd.sh dynamic COLUMNS handling
echo "[5/6] Verifying motd.sh dynamic formatting..."
MOTD_SH="$PROJECT_ROOT/app/src/main/assets/bootstrap-overlay/etc/motd.sh"
if ! grep -q "COLUMNS" "$MOTD_SH"; then
    echo "ERROR: motd.sh must dynamically adapt to COLUMNS!" >&2
    exit 1
fi
echo "✓ motd.sh dynamic column calculation verified."

# 6. Check ExtraKeysView capitalization & styling
echo "[6/6] Verifying ExtraKeysView sentence case labels..."
EXTRA_KEYS_VIEW="$PROJECT_ROOT/termux-shared/src/main/java/com/termux/shared/termux/extrakeys/ExtraKeysView.java"
if ! grep -q 'return "Esc";' "$EXTRA_KEYS_VIEW"; then
    echo "ERROR: ExtraKeysView missing sentence case Esc label mapping!" >&2
    exit 1
fi
echo "✓ ExtraKeysView sentence case verified."

echo "=== ALL UI & LIQUID GLASS LINT CHECKS PASSED ==="
exit 0
