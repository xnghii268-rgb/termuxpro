# Asset Provenance & License Catalog

All assets added or updated in this phase are 100% license-free, original vector work created specifically for Termux Pro.
NO Apple assets, logos, or proprietary fonts were used.

## 1. Vector Icons & Shapes (Original Custom Work)
- **Design Specifications**: 24x24dp viewport, 2.0dp - 2.2dp stroke width, rounded caps and joins, monochrome palette (`#F2EDE4` warm cream / `#3DDC97` mint accent).
- `ic_prompt_chevron.xml`: Terminal prompt chevron `>_` (replaces legacy media-play arrow). Original vector.
- `ic_plus_new.xml`: Clean 2.2dp stroke plus sign for session creation. Original vector.
- `ic_chevron_left.xml` / `ic_chevron_right.xml`: Thin 2.0dp stroke chevron for navigation back and forward. Original vector.
- `ic_more_dots.xml`: 3-dot overflow options glyph. Original vector.
- `ic_tab_home.xml`: Minimalist home shelter glyph. Original vector.
- `ic_tab_terminal.xml`: Classic terminal command glyph. Original vector.
- `ic_tab_utilities.xml`: 4-squircle bento grid glyph. Original vector.
- `ic_tab_settings.xml`: Balanced gear glyph. Original vector.
- `ic_ai_agent.xml`: Multi-point sparkle diamond for AI coding. Original vector.
- `bg_glass_round_btn.xml`: 44dp circular glass button with translucent graphite fill and 1dp specular rim.
- `glass_drawer_background.xml`: 28dp squircle drawer background with 1.2dp specular border.
- `bg_session_chips_bar.xml`: Translucent glass bar with bottom border for top terminal chrome.

## 2. Typography
- Monospace font for terminal code transcript preview and terminal emulator (`android:fontFamily="monospace"` / `assets/fonts/termuxpro-mono.ttf`).
- System UI: Clean sans-serif with full Vietnamese diacritic coverage (`Ắ Ử Ỡ ệ ợ ẫ Đ Ơ Ư`).

## 3. Visual & Motion Shaders
- `LiquidBackdropView`: Custom canvas-rendered optical living background with 3 procedural soft orbs (warm cream, champagne, mint) + faint scanline grid + gyroscope parallax. Zero external binaries.
- `LiquidTabBar`: Custom canvas-rendered pill tab bar with damped spring liquid droplet physics. Zero external dependencies.
- `GlassComponents`: Reusable glass buttons, confirm dialogs, and toast notifications adhering to Liquid Glass design tokens.
