### fastGQL

Realtime GraphQL on PostgreSQL / MySQL

#### Development

Check style:

```shell script
./gradlew clean checkstyleMain
```

Start Postgres container:

```shell script
scripts/start_postgres.sh
```

Start FastGQL in dev mode with hot reload:

```shell script
./gradlew vertxRun
````

Go to [localhost:8080/graphiql/](http://localhost:8080/graphiql/) or query ```localhost:8080/graphql```

#### Production

Build production bundle:

```shell script
./gradlew installDist
```

Execute production version:

```shell script
build/install/fastgql/bin/fastgql run --conf src/main/resources/conf-postgres.json dev.fastgql.FastGQL
```
