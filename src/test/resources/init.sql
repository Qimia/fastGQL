DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS addresses;

CREATE TABLE addresses(
  id INT PRIMARY KEY,
  street VARCHAR(255),
  house_number INT
);

CREATE TABLE customers(
  id INT PRIMARY KEY,
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  email VARCHAR(255),
  address INT REFERENCES addresses(id)
);

INSERT INTO addresses VALUES (101, 'Astreet', 7);
INSERT INTO addresses VALUES (102, 'Bstreet', 6);
INSERT INTO addresses VALUES (103, 'Cstreet', 5);
INSERT INTO customers VALUES (101, 'John', 'Adam', 'john@adam.com', 101);
INSERT INTO customers VALUES (102, 'Uli', 'Werk', 'uli@werk.com', 101);
INSERT INTO customers VALUES (103, 'a', 'D', 'a@D.com', 101);
INSERT INTO customers VALUES (104, 'b', 'C', 'b@C.com', 102);
INSERT INTO customers VALUES (105, 'c', 'B', 'c@B.com', 102);
INSERT INTO customers VALUES (106, 'd', 'A', 'd@A.com', 102);