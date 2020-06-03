### fastGQL

Realtime GraphQL on PostgreSQL / MySQL

#### Quick start

Run all tests:

```shell script
./gradlew test
```

#### Development

Start Postgres container:

```shell script
./start_postgres.sh
```

Start FastGQL in dev mode with hot reload:

```shell script
./gradlew vertxRun
````

#### Production

Build production bundle:

```shell script
./gradlew installDist
```

Execute production version:

```shell script
build/install/fastgql/bin/fastgql run --conf src/main/conf.json dev.fastgql.FastGQL
```