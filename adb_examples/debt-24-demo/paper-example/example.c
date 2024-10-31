#include <stdio.h>
#include <pthread.h>

pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
typedef enum { PUBLISH, CACHE } ThreadAction;
int global;

int f(ThreadAction action) {
    int cache = 0;
    switch (action) {
        case CACHE:
            printf("Store in local cache!\n");
            cache = 42;
        case PUBLISH:
            printf("Publish work!\n");
            global = 42;
    }
}

void *t(void *arg) {
    if (pthread_mutex_trylock(&mutex)) {
        f(CACHE);
    } else {
        f(PUBLISH);
        pthread_mutex_unlock(&mutex);
    }
}


int main() {
    pthread_t t1, t2;
    pthread_create(&t1, NULL, t, NULL);
    pthread_create(&t2, NULL, t, NULL);
    pthread_join(t1, NULL);
    pthread_join(t2, NULL);
    return 0;
}
