DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS addresses;
DROP TABLE IF EXISTS temp;

CREATE TABLE addresses(
  id INT PRIMARY KEY,
  id2 INT UNIQUE,
  street VARCHAR(255),
  house_number INT
);

CREATE TABLE customers(
  id INT PRIMARY KEY,
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  email VARCHAR(255),
  address INT REFERENCES addresses(id),
  address2 INT REFERENCES addresses(id2)
);

INSERT INTO addresses VALUES (101, 101, 'Astreet', 5);
INSERT INTO addresses VALUES (102, 102, 'Bstreet', 6);
INSERT INTO customers VALUES (101, 'John', 'Adam', 'john@adam.com', 101, 101);
INSERT INTO customers VALUES (102, 'Uli', 'Werk', 'uli@werk.com', 102, 101);
INSERT INTO customers VALUES (103, 'Mark', 'Woy', 'mark@woy.com');

