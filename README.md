## fastGQL

Realtime GraphQL on PostgreSQL / MySQL

### About

This project exposes PostgreSQL / MySQL through GraphQL API, with built-in authorization engine. Inspired by [Hasura](https://hasura.io/).

This is alpha-version, with some features still in development:

- [x] basic operations
  - [x] query
  - [x] mutate
    - [x] insert
    - [ ] update
  - [ ] delete
  - [x] subscription
- [x] JWT authentication
- [x] JWT authorization
  - [x] query
  - [x] mutate
  - [ ] delete
  - [ ] subscription
- [x] authorization engine configured with groovy DSL
  - [x] role-based column access
  - [x] role-based and content-based row access 
- [x] user-defined filtering via GraphQL arguments
  - [x] where
  - [x] order by
  - [x] limit
  - [x] offset

### Example

#### Database

```postgresql
CREATE TABLE addresses(
  id INT PRIMARY KEY,
  street VARCHAR(255)
);

CREATE TABLE customers(
  id INT PRIMARY KEY,
  name VARCHAR(255),
  address INT REFERENCES addresses(id)
);

INSERT INTO addresses VALUES (0, 'Danes Hill');
INSERT INTO addresses VALUES (1, 'Rangewood Road');
INSERT INTO customers VALUES (0, 'Stacey', 0);
INSERT INTO customers VALUES (1, 'John', 0);
INSERT INTO customers VALUES (2, 'Daniele', 1);
```

#### Query with simple permissions

##### Server-side permissions

<table style="width:100%">
  <tr>
    <th colspan="2">Permissions</th>
  </tr>
  <tr>
    <td colspan="2"><pre lang="groovy">permissions {
  role ('default') {
    table ('customers') {
      ops ([select]) {
          allow 'id', 'name', 'address'
      }
    }
    table ('addresses') {
      ops ([select]) {
          allow 'id', 'street'
      }
    }
  }
}</pre>
    </td>
  </tr>
  <tr>
    <th>Query</th>
    <th>Response</th>
  </tr>
  <tr>
    <td>
      <pre lang="graphql">query {
  customers {
    id
    name
    address
    address_ref {
      id
      street
      customers_on_address {
        id
      }
    }
  }
}</pre>
    </td>
    <td><pre lang="json">{
  "data": {
    "customers": [
      {
        "id": 0,
        "name": "Stacey",
        "address": 0,
        "address_ref": {
          "id": 0,
          "street": "Danes Hill",
          "customers_on_address": [
            {
              "id": 0
            },
            {
              "id": 1
            }
          ]
        }
      },
      {
        "id": 1,
        "name": "John",
        "address": 0,
        "address_ref": {
          "id": 0,
          "street": "Danes Hill",
          "customers_on_address": [
            {
              "id": 0
            },
            {
              "id": 1
            }
          ]
        }
      },
      {
        "id": 2,
        "name": "Daniele",
        "address": 1,
        "address_ref": {
          "id": 1,
          "street": "Rangewood Road",
          "customers_on_address": [
            {
              "id": 2
            }
          ]
        }
      }
    ]
  }
}</pre>
    </td>
  </tr>
</table>

| b

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
