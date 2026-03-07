void f(int x) {
    assert(x - 1 < x); // Using breakpoint: two different contexts will be displayed in the "call stack" panel
    if (x > 1 ) {
        printf("Hello!");
    }
}

int main() {
    f(rand() % 10);
    f(42);
    return 0;
}