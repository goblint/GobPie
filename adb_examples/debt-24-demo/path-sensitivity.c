#include <pthread.h>

pthread_mutex_t mutex; 

int main(void) {
    int do_work = rand();
    int work = 0;
    if (do_work) {
        pthread_mutex_lock(&mutex);
    }

    // ...

    if (do_work) {
        work++;
        pthread_mutex_unlock(&mutex);
    }
}