DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS addresses;
DROP TABLE IF EXISTS phones;

CREATE TABLE phones(
  id INT PRIMARY KEY,
  phone INT
);

CREATE TABLE addresses(
  id INT PRIMARY KEY,
  street VARCHAR(255)
);

CREATE TABLE customers(
  id INT PRIMARY KEY,
  name VARCHAR(255),
  address INT REFERENCES addresses(id),
  phone INT REFERENCES phones(id)
);

INSERT INTO addresses VALUES (0, 'Danes Hill');
INSERT INTO addresses VALUES (1, 'Rangewood Road');
INSERT INTO customers VALUES (0, 'Stacey', 0);
INSERT INTO customers VALUES (1, 'John', 0);
INSERT INTO customers VALUES (2, 'Daniele', 1);

