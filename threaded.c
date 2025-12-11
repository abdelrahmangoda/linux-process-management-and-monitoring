#include <stdio.h>
#include <pthread.h>
#include <unistd.h>

void* worker(void* arg) {
    long id = (long)arg;

    // CPU-heavy loop to show high CPU usage per thread
    while (1) {
        for (volatile long i = 0; i < 100000000; i++);
    }

    return NULL;
}

int main() {
    pthread_t threads[4];
    int num_threads = 4;

    printf("Main PID: %d\n", getpid());
    printf("Creating %d CPU-heavy threads...\n", num_threads);

    // Create threads
    for (long i = 0; i < num_threads; i++) {
        pthread_create(&threads[i], NULL, worker, (void*)i);
        printf("Thread %ld created\n", i);
    }

    // Optional: wait for threads (not necessary for CPU load)
    for (int i = 0; i < num_threads; i++) {
        pthread_join(threads[i], NULL);
    }

    return 0;
}
