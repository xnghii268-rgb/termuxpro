#!/data/data/com.termuxpro.app/files/usr/bin/bash
# Termux Pro Hacker Console MOTD (Ultra-fast: <20ms)

USER_NAME="${USER:-user}"
HOST_NAME="${HOSTNAME:-android}"
DATE_STR=$(date +"%H:%M • %d/%m/%Y" 2>/dev/null || echo "")

# Fast battery check
BAT_VAL=""
if [ -f /sys/class/power_supply/battery/capacity ]; then
    BAT_VAL="$(cat /sys/class/power_supply/battery/capacity 2>/dev/null)%"
fi

# Fast local IP
IP_VAL="$(ip route get 1.1.1.1 2>/dev/null | awk '{print $7}' | head -n1)"
[ -z "$IP_VAL" ] && IP_VAL="127.0.0.1"

# Rotating Tips (1-10)
TIPS=(
    "Dùng phím [Dán ảnh] trên bàn phím phụ để chèn nhanh ảnh cho AI CLI"
    "Chạy lệnh 'tp' hoặc 'menu' bất kỳ lúc nào để mở bảng điều khiển này"
    "Gõ 'agy' để tương tác trực tiếp với trợ lý lập trình Antigravity"
    "Dùng phím [Lệnh] để mở nhanh bảng lệnh tương tác Palette"
    "Sao chép ảnh chụp màn hình gần nhất bằng lệnh shell 'lastshot'"
    "Chạy 'pkg update' để đồng bộ kho gói Termux Pro mới nhất"
    "Nhấn giữ phím [Dán ảnh] để chọn ảnh tùy ý từ bộ sưu tập hệ thống"
    "Các tệp đã sao chép được lưu an toàn tại thư mục ~/.clip"
    "Cài đặt gói lập trình với lệnh 'pkg install python git nodejs-lts'"
    "Cần trợ giúp hoặc đóng góp? Xem mã nguồn mở tại github.com/xnghii268-rgb/termuxpro"
)
DAY_NUM=$(date +%d 2>/dev/null || echo "1")
DAY_NUM=$((10#$DAY_NUM % 10))
CHOSEN_TIP="${TIPS[$DAY_NUM]}"

# Render Box Frame
C_BORDER="\e[38;5;37m"
C_TITLE="\e[1;38;5;81m"
C_LABEL="\e[1;38;5;222m"
C_ACCENT="\e[1;38;5;48m"
C_MUTED="\e[38;5;246m"
C_RESET="\e[0m"

echo -e "${C_BORDER}╭────────────────────────────────────────────────────────────╮${C_RESET}"
echo -e "${C_BORDER}│${C_TITLE}                  TERMUX PRO  •  CONSOLE                    ${C_BORDER}│${C_RESET}"
echo -e "${C_BORDER}├────────────────────────────────────────────────────────────┤${C_RESET}"
printf "${C_BORDER}│${C_RESET}  ${C_LABEL}Người dùng:${C_RESET} %-16s ${C_LABEL}Thời gian:${C_RESET} %-18s ${C_BORDER}│\n${C_RESET}" "${USER_NAME}@${HOST_NAME}" "$DATE_STR"
printf "${C_BORDER}│${C_RESET}  ${C_LABEL}Địa chỉ IP:${C_RESET} %-16s ${C_LABEL}Pin:${C_RESET}       %-18s ${C_BORDER}│\n${C_RESET}" "$IP_VAL" "${BAT_VAL:-100%}"
echo -e "${C_BORDER}├────────────────────────────────────────────────────────────┤${C_RESET}"
echo -e "${C_BORDER}│${C_ACCENT}  PHÍM TẮT TRUY CẬP NHANH (Gõ 'tp' để mở menu):             ${C_BORDER}│${C_RESET}"
echo -e "${C_BORDER}│${C_RESET}   [1] Cài AntiGravity CLI     [6] Cập nhật hệ thống (pkg)  ${C_BORDER}│${C_RESET}"
echo -e "${C_BORDER}│${C_RESET}   [2] Môi trường Python       [7] Dán ảnh gần nhất         ${C_BORDER}│${C_RESET}"
echo -e "${C_BORDER}│${C_RESET}   [3] Môi trường Node.js      [8] Sao lưu Termux Pro       ${C_BORDER}│${C_RESET}"
echo -e "${C_BORDER}│${C_RESET}   [4] Công cụ Git & GitHub    [9] Đổi giao diện hiển thị   ${C_BORDER}│${C_RESET}"
echo -e "${C_BORDER}│${C_RESET}   [5] Soạn thảo Nano / Vim    [0] Thông tin bản quyền      ${C_BORDER}│${C_RESET}"
echo -e "${C_BORDER}├────────────────────────────────────────────────────────────┤${C_RESET}"
printf "${C_BORDER}│${C_MUTED}  Mẹo: %-53s ${C_BORDER}│\n${C_RESET}" "$CHOSEN_TIP"
echo -e "${C_BORDER}╰────────────────────────────────────────────────────────────╯${C_RESET}"
