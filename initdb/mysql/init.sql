CREATE DATABASE test;
USE test;

DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS addresses;

CREATE TABLE addresses(
  id INT PRIMARY KEY,
  street VARCHAR(255)
);

CREATE TABLE customers(
  id INT PRIMARY KEY,
  name VARCHAR(255),
  address INT,
  FOREIGN KEY (address) REFERENCES addresses(id)
);

INSERT INTO addresses VALUES (0, 'Danes Hill');
INSERT INTO addresses VALUES (1, 'Rangewood Road');
INSERT INTO customers VALUES (0, 'Stacey', 0);
INSERT INTO customers VALUES (1, 'John', 0);
INSERT INTO customers VALUES (2, 'Daniele', 1);
