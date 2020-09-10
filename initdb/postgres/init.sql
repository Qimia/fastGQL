DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS addresses;
DROP TABLE IF EXISTS phones;

CREATE TABLE phones(
  id INT PRIMARY KEY,
  phone INT
);

CREATE TABLE addresses(
  id INT PRIMARY KEY,
  id2 INT UNIQUE,
  street VARCHAR(255),
  house_number INT,
  phone INT REFERENCES phones(id)
);

CREATE TABLE customers(
  id INT PRIMARY KEY,
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  email VARCHAR(255),
  address INT REFERENCES addresses(id),
  address2 INT REFERENCES addresses(id2)
);

INSERT INTO phones VALUES (101, 123123);
INSERT INTO phones VALUES (102, 321321);

INSERT INTO addresses VALUES (101, 101, 'Astreet', 5, 101);
INSERT INTO addresses VALUES (102, 102, 'Bstreet', 6, 102);
INSERT INTO addresses VALUES (103, NULL, 'Bstreet', 6, 101);
INSERT INTO customers VALUES (101, 'John', 'Adam', 'john@adam.com', 101, 101);
INSERT INTO customers VALUES (102, 'Uli', 'Werk', 'uli@werk.com', 102, null);
INSERT INTO customers VALUES (103, 'Mark', 'Woy', 'mark@woy.com', null, 101);

