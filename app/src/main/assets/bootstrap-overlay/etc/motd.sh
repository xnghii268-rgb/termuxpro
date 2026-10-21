#!/data/data/com.termuxpro.app/files/usr/bin/bash
# Termux Pro Hacker Console Banner (Clean Rule Layout, No Misaligned Borders)

export LANG="C.UTF-8"
export LC_ALL="C.UTF-8"

COLS="${COLUMNS:-$(tput cols 2>/dev/null || stty size 2>/dev/null | awk '{print $2}' || echo "")}"
[ -z "$COLS" ] || [ "$COLS" -le 0 ] 2>/dev/null && COLS=80

# 1-line fallback for small screens or dumb terminals
if [ "$TERM" = "dumb" ] || [ -n "$NO_COLOR" ] || [ "$COLS" -lt 38 ]; then
    echo "Termux Pro • Gõ 'tp' để mở bảng điều khiển"
    exit 0
fi

INNER_WIDTH=$((COLS - 2))
[ "$INNER_WIDTH" -lt 10 ] && INNER_WIDTH=10
MAX_LINE=$((COLS - 4))

# Brand accent #FF3C70
C_ACCENT="\e[38;2;255;60;112m"
C_TITLE="\e[1;38;2;255;60;112m"
C_LABEL="\e[1;38;5;222m"
C_MUTED="\e[38;5;246m"
C_RESET="\e[0m"

RULE_LINE=$(printf "─%.0s" $(seq 1 "$INNER_WIDTH"))

print_line() {
    local text="$1"
    local color="${2:-$C_RESET}"
    local vis_len=${#text}
    if [ "$vis_len" -gt "$MAX_LINE" ]; then
        text="${text:0:$((MAX_LINE - 1))}…"
    fi
    echo -e "  ${color}${text}${C_RESET}"
}

# Real values only - hide field when unavailable
U=$(id -un 2>/dev/null || whoami 2>/dev/null || echo "")
H=$(hostname 2>/dev/null || echo "")
[ "$H" = "localhost" ] || [ "$H" = "127.0.0.1" ] && H=""
USER_STR=""
if [ -n "$U" ] && [ -n "$H" ]; then
    USER_STR="${U}@${H}"
elif [ -n "$U" ]; then
    USER_STR="$U"
fi

TIME_STR=$(date +"%H:%M • %d/%m/%Y" 2>/dev/null || echo "")

IP_STR="$(ip route get 1.1.1.1 2>/dev/null | awk '{print $7}' | head -n1)"
if [ -z "$IP_STR" ] || [ "$IP_STR" = "127.0.0.1" ]; then
    IP_STR=""
fi

BAT_STR=""
if [ -f /sys/class/power_supply/battery/capacity ]; then
    BAT_VAL="$(cat /sys/class/power_supply/battery/capacity 2>/dev/null || true)"
    [ -n "$BAT_VAL" ] && BAT_STR="${BAT_VAL}%"
fi
if [ -z "$BAT_STR" ] && command -v termux-battery-status >/dev/null 2>&1; then
    BAT_JSON=$(termux-battery-status 2>/dev/null || true)
    if [ -n "$BAT_JSON" ]; then
        BAT_VAL=$(echo "$BAT_JSON" | grep -o '"percentage": *[0-9]*' | awk -F: '{print $2}' | tr -d ' ')
        [ -n "$BAT_VAL" ] && BAT_STR="${BAT_VAL}%"
    fi
fi

# Rotating Tips (1-10)
TIPS=(
    "Dùng phím [Dán ảnh] trên bàn phím phụ để chèn nhanh ảnh cho AI CLI"
    "Chạy lệnh 'tp' hoặc 'menu' bất kỳ lúc nào để mở bảng điều khiển"
    "Gõ 'agy' để tương tác trực tiếp với trợ lý lập trình Antigravity"
    "Dùng phím [Lệnh] để mở nhanh bảng lệnh tương tác Palette"
    "Sao chép ảnh chụp màn hình gần nhất bằng lệnh shell 'lastshot'"
    "Chạy 'pkg update' để đồng bộ kho gói Termux Pro mới nhất"
    "Nhấn giữ phím [Dán ảnh] để chọn ảnh tùy ý từ bộ sưu tập"
    "Các tệp đã sao chép được lưu an toàn tại thư mục ~/.clip"
    "Cài đặt gói lập trình với lệnh 'pkg install python git nodejs-lts'"
    "Cần trợ giúp hoặc đóng góp? Xem mã nguồn mở tại github.com/xnghii268-rgb/termuxpro"
)
DAY_NUM=$(date +%d 2>/dev/null || echo "1")
DAY_NUM=$((10#$DAY_NUM % 10))
CHOSEN_TIP="${TIPS[$DAY_NUM]}"

# Render Top Rule
echo -e "${C_ACCENT}${RULE_LINE}${C_RESET}"
echo -e "  ${C_TITLE}TERMUX PRO • CONSOLE${C_RESET}"

# Info row
if [ -n "$USER_STR" ] || [ -n "$TIME_STR" ]; then
    if [ "$COLS" -ge 60 ] && [ -n "$USER_STR" ] && [ -n "$TIME_STR" ]; then
        l_text="Người dùng: $USER_STR"
        r_text="Thời gian: $TIME_STR"
        col1_w=28
        l_len=${#l_text}
        pad=""
        [ "$l_len" -lt "$col1_w" ] && pad=$(printf "%*s" "$((col1_w - l_len))" "")
        print_line "${l_text}${pad}  ${r_text}" "$C_LABEL"
    else
        [ -n "$USER_STR" ] && print_line "Người dùng: $USER_STR" "$C_LABEL"
        [ -n "$TIME_STR" ] && print_line "Thời gian:  $TIME_STR" "$C_LABEL"
    fi
fi

if [ -n "$IP_STR" ] || [ -n "$BAT_STR" ]; then
    if [ "$COLS" -ge 60 ] && [ -n "$IP_STR" ] && [ -n "$BAT_STR" ]; then
        l_text="Địa chỉ IP: $IP_STR"
        r_text="Pin: $BAT_STR"
        col1_w=28
        l_len=${#l_text}
        pad=""
        [ "$l_len" -lt "$col1_w" ] && pad=$(printf "%*s" "$((col1_w - l_len))" "")
        print_line "${l_text}${pad}  ${r_text}" "$C_LABEL"
    else
        [ -n "$IP_STR" ] && print_line "Địa chỉ IP: $IP_STR" "$C_LABEL"
        [ -n "$BAT_STR" ] && print_line "Pin:        $BAT_STR" "$C_LABEL"
    fi
fi

echo ""
print_line "MENU NHANH (Gõ 'tp' để mở):" "$C_ACCENT"

M1="[1] Cài AntiGravity"
M2="[2] Python"
M3="[3] Node.js"
M4="[4] Git và GitHub"
M5="[5] Nano / Vim"
M6="[6] Cập nhật gói"
M7="[7] Dán ảnh"
M8="[8] Sao lưu"
M9="[9] Giao diện"
M0="[0] Bản quyền"

if [ "$COLS" -ge 50 ]; then
    col1_w=24
    p_menu() {
        local left="$1"
        local right="$2"
        local l_len=${#left}
        local pad=""
        [ "$l_len" -lt "$col1_w" ] && pad=$(printf "%*s" "$((col1_w - l_len))" "")
        print_line "${left}${pad}  ${right}" "$C_RESET"
    }
    p_menu "$M1" "$M6"
    p_menu "$M2" "$M7"
    p_menu "$M3" "$M8"
    p_menu "$M4" "$M9"
    p_menu "$M5" "$M0"
else
    print_line "$M1"
    print_line "$M2"
    print_line "$M3"
    print_line "$M4"
    print_line "$M5"
    print_line "$M6"
    print_line "$M7"
    print_line "$M8"
    print_line "$M9"
    print_line "$M0"
fi

echo ""
print_line "Mẹo: $CHOSEN_TIP" "$C_MUTED"

# Render Bottom Rule
echo -e "${C_ACCENT}${RULE_LINE}${C_RESET}"
