### fastGQL

Realtime GraphQL on PostgreSQL / MySQL

#### Quick start

Run all tests:

```shell script
./gradlew test
```

Start in dev mode with hot reload:

```shell script
./gradlew vertxRun
````

Build production bundle:

```shell script
./gradlew installDist
```

Execute production version:

```shell script
build/install/fastgql/bin/fastgql run --conf src/main/conf.json ai.qimia.fastgql.oldarch.GraphQLServer
```