DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS addresses;

CREATE TABLE customers(
  id INT PRIMARY KEY,
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  email VARCHAR(255)
);

CREATE TABLE addresses(
  id INT PRIMARY KEY,
  street VARCHAR(255),
  house_number INT
);

INSERT INTO customers VALUES (101, 'John', 'Adam', 'john@adam.com');
INSERT INTO customers VALUES (102, 'Uli', 'Werk', 'uli@werk.com');
INSERT INTO addresses VALUES (101, 'Astreet', 5);
INSERT INTO addresses VALUES (102, 'Bstreet', 6);

