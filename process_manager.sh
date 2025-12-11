#!/bin/bash

# ======================
# Paths to executables
# ======================
CPU_HOG="./cpu_hog"
SLOW="./slow_process"
ZOMBIE="./zombie"
THREADED="./threaded"

# ======================
# Show processes sorted by CPU
# ======================
show_processes() {
    echo "--------------------------------------------"
    echo "   Running Processes Sorted by CPU Usage"
    echo "--------------------------------------------"
    ps -eo pid,ppid,cmd,%cpu,%mem,stat --sort=-%cpu | head -15
    echo "--------------------------------------------"
}

# ======================
# Start functions
# ======================
start_cpu_hog() {
    echo "[+] Starting CPU Hog..."
    $CPU_HOG &
}

start_slow() {
    echo "[+] Starting slow process..."
    $SLOW &
}

start_zombie() {
    echo "[+] Starting zombie process..."
    $ZOMBIE &
}

start_threaded() {
    echo "[+] Starting threaded process..."
    $THREADED &
}

# ======================
# Renice function
# ======================
renice_process() {
    read -p "Enter PID to renice: " PID
    read -p "Enter new nice value (e.g., 10): " NICE
    sudo renice "$NICE" -p "$PID"
}

# ======================
# Send signal function
# ======================
send_signal() {
    read -p "Enter PID: " PID
    echo "1) SIGTERM"
    echo "2) SIGKILL"
    read -p "Choose signal: " SIG

    if [ "$SIG" == "1" ]; then
        kill -TERM "$PID"
    elif [ "$SIG" == "2" ]; then
        kill -KILL "$PID"
    else
        echo "Invalid choice."
    fi
}

# ======================
# Main Menu
# ======================
while true; do
    echo ""
    echo "========== Process Manager =========="
    echo "1) Show processes"
    echo "2) Start CPU Hog"
    echo "3) Start Slow Process"
    echo "4) Start Zombie"
    echo "5) Start Threaded Program"
    echo "6) Renice Process"
    echo "7) Send Signal"
    echo "8) Exit"
    echo "====================================="
    read -p "Choose an option: " OPTION

    case $OPTION in
        1) show_processes ;;
        2) start_cpu_hog ;;
        3) start_slow ;;
        4) start_zombie ;;
        5) start_threaded ;;
        6) renice_process ;;
        7) send_signal ;;
        8) exit 0 ;;
        *) echo "Invalid option." ;;
    esac
done
