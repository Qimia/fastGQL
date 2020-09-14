
permissions {
    table ('customers') {
        role ('default') {
            ops ([select]) {
                check 'id' gt 100
            }
        }
    }
}
