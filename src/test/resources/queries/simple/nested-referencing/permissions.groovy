
permissions {
    role ('default') {
        table ('customers') {
            ops([select]) {
                allow 'id'
            }
        }
        table ('addresses') {
            ops([select]) {
                allow 'id'
            }
        }
        table ('streets') {
            ops([select]) {
                allow 'id'
            }
        }
    }
}