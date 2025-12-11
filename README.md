# Linux Process Management and Monitoring
This repository contains implementations for Linux process creation, 
monitoring, scheduling, and visualization as required in the Operating Systems course project.

## 📌 Project Components

### 1. CPU-Intensive Process
- A C program that consumes maximum CPU cycles.
- Used to observe CPU scheduling and effect of `nice` values.

### 2. Slow / Idle Process
- Sleeps most of the time.
- Handles `SIGTERM` and demonstrates difference from `SIGKILL`.

### 3. Zombie Process Creation
- Parent sleeps while child finishes → zombie creation.
- Can be observed using `ps`, `top`, or `htop`.

### 4. Threaded Process
- Multithreaded program using POSIX `pthread` library.
- Used to observe scheduling across threads.

### 5. Process Manager Script
A Bash script that allows:
- Listing processes with PID, PPID, state, CPU%, memory, nice value.
- Launching CPU hog, slow, zombie, or threaded processes.
- Renicing processes.
- Sending signals (`SIGTERM`, `SIGKILL`).

### 6. GUI / Gantt Chart Visualization
- Java-based GUI using Swing / JavaFX.
- Displays:
  - PID
  - Command
  - CPU%
  - Memory
  - State
  - Priority
- Includes a simple Gantt-like CPU usage visualization.

---

## 🛠 Technologies Used
- Linux (Ubuntu recommended)
- GCC Compiler
- Bash Shell
- POSIX Threads
- Java (Swing/JavaFX)
- Linux tools: `ps`, `top`, `htop`

---

## ▶️ How to Run

### Compile Programs
gcc cpu_hog.c -o cpu_hog
gcc slow_process.c -o slow_process
gcc zombie_process.c -o zombie_process
gcc threaded_process.c -pthread -o threaded_process


### Run Script
chmod +x process_manager.sh
./process_manager.sh


### Run GUI
cd gui
javac ProcessGUI.java
java ProcessGUI



