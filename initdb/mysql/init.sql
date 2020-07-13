CREATE DATABASE test;
USE test;

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
INSERT INTO addresses VALUES (103, NULL, 'Bstreet', 6);
INSERT INTO customers VALUES (101, 'John', 'Adam', 'john@adam.com', 101, 101);
INSERT INTO customers VALUES (102, 'Uli', 'Werk', 'uli@werk.com', 102, null);
INSERT INTO customers VALUES (103, 'Mark', 'Woy', 'mark@woy.com', null, 101);
INSERT INTO customers VALUES (104, 'd', 'D', 'd@D.com', 101, null);
INSERT INTO customers VALUES (105, 'e', 'E', 'e@E.com', 102, null);
INSERT INTO customers VALUES (106, 'f', 'F', 'f@F.com', 101, null);
INSERT INTO customers VALUES (107, 'g', 'G', 'g@G.com', 102, null);
INSERT INTO customers VALUES (108, 'h', 'H', 'h@H.com', 102, null);

