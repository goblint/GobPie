#include "precise_stdlib.h"

struct s {
    int n;
    int m[3];
};

int main() {
    int a = rand() % 19;
    int b = 79;
    struct s c = {
        7 * a + 5,
        {b, b + 1, b + 2}
    };
    return 0;
}