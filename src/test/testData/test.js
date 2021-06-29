function bla(n) {
    const x = n + 2;
    let y = 42;
    console.log(x);
    y = Math.min(2, n);
    if (n === 2) {
        return bla(y - 2);
    }
    return n - 1;
}
