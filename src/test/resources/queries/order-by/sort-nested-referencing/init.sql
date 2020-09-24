DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS addresses CASCADE;
DROP TABLE IF EXISTS streets CASCADE;

CREATE TABLE streets(
  id INT PRIMARY KEY,
  name VARCHAR(255)
);

CREATE TABLE addresses(
  id INT PRIMARY KEY,
  street INT,
  FOREIGN KEY (street) REFERENCES streets(id)
);

CREATE TABLE customers(
  id INT PRIMARY KEY,
  address INT,
  FOREIGN KEY (address) REFERENCES addresses(id)
);

INSERT INTO streets VALUES (0, 'a');
INSERT INTO streets VALUES (1, 'b');
INSERT INTO streets VALUES (2, 'c');
INSERT INTO streets VALUES (3, 'd');
INSERT INTO addresses VALUES (0, 0);
INSERT INTO addresses VALUES (1, 1);
INSERT INTO addresses VALUES (2, 2);
INSERT INTO addresses VALUES (3, 3);
INSERT INTO customers VALUES (0, 2);
INSERT INTO customers VALUES (1, 3);
INSERT INTO customers VALUES (2, 0);
INSERT INTO customers VALUES (3, 1);
