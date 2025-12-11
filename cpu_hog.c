#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>

int main() {
    printf("CPU-intensive process started. PID: %d\n", getpid());
    while(1);
    return 0;
}

