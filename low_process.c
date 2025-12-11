#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <unistd.h>
#include <time.h>


void handle_sigterm(int sig) {
    printf("\n[%ld] Received SIGTERM, shutting down gracefully...\n", time(NULL));
    exit(0);
}


int main() {

    signal(SIGTERM, handle_sigterm);

    pid_t pid = getpid();
    printf("Slow process started. PID: %d\n", pid);
    printf("Use 'kill -TERM %d' for graceful shutdown or 'kill -KILL %d' for force kill.\n", pid, pid);


    while(1) {
        sleep(1);  
    }

    return 0;
}
