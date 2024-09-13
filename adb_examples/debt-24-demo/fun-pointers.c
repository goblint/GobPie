void f(int x) {
  printf("%i", x);
}

void g(int x) {
  printf("%i", x + 100);
}

int main() {
  int i = rand() % 100;
  void (*fp)(int);
  if (i >= 50)
    fp = &f;
  else
    fp = &g;
  fp(i - 30); // Using breakpoint: step into is unambiguous and requests "step into target"
}