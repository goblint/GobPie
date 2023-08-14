#include <stdlib.h>
#include <stdio.h>
#include <pthread.h>

pthread_mutex_t lukk = PTHREAD_MUTEX_INITIALIZER;

void f(int a) {
    printf("%i", a);
}

void g(int b) {
    if (b >= 50) {
        f(b - 50);
    } else {
        f(b);
        f(b + 12);
    }
}

void h(int c) {
    if (c == 0) {
        pthread_mutex_lock(&lukk);
    }
    if (c > 0) {
        printf("%i", c);
    }
    if (c == 0) {
        pthread_mutex_unlock(&lukk);
    }
}

int main() {
    int i = rand() % 100;
    g(i);
    h(i);
}