#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>

int main() {
    pid_t pid = fork();  

    if (pid < 0) {
        // fork failed
        perror("fork failed");
        exit(1);
    }

    if (pid == 0) {
        // Child process
        printf("Child process: PID=%d, exiting immediately\n", getpid());
        exit(0);  
    } else {
        // Parent process
        printf("Parent process: PID=%d, Child PID=%d\n", getpid(), pid);

        // Sleep without calling wait() so that child becomes a zombie
        printf("Parent sleeping for 30  seconds. Child will be zombie now.\n");
        sleep(30);


        int status;
        waitpid(pid, &status, 0);  

       printf("Parent done. Zombie reaped if waitpid() was called.\n");
    }

    return 0;
}
