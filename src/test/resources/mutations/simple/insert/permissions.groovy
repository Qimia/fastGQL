
permissions {
    role ('default') {
        table ('customers') {
            ops([select]) {
                allow 'id'
            }
        }
        table ('addresses') {
            ops([select, insert]) {
                allow 'id'
            }
        }
    }
}