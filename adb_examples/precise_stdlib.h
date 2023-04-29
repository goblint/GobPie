// Goblint cannot by itself tell that builtin functions such as rand() return only positive values.
// This redefines rand in a way that allows Goblint to infer a more precise return value range for rand().

int rand() {
    int r;
    return r < 0 ? 0 : r;
}