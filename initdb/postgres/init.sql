DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS addresses;
DROP TABLE IF EXISTS phones;

CREATE TABLE phones(
  id INT PRIMARY KEY,
  phone INT
);

CREATE TABLE addresses(
  id INT PRIMARY KEY,
  street VARCHAR(255),
  phone INT REFERENCES phones(id)
);

CREATE TABLE customers(
  id INT PRIMARY KEY,
  name VARCHAR(255),
  address INT REFERENCES addresses(id)
);

INSERT INTO phones VALUES (0, 123);
INSERT INTO phones VALUES (1, 234);
INSERT INTO phones VALUES (2, 345);
INSERT INTO addresses VALUES (0, 'Danes Hill', 0);
INSERT INTO addresses VALUES (1, 'Rangewood Road', 1);
INSERT INTO addresses VALUES (2, 'Random Road', 2);
INSERT INTO customers VALUES (0, 'Stacey', 0);
INSERT INTO customers VALUES (1, 'John', 1);
INSERT INTO customers VALUES (2, 'Daniele', 2);

