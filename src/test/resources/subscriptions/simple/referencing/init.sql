DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS addresses CASCADE;
DROP TABLE IF EXISTS streets CASCADE;

CREATE TABLE addresses(
  id INT PRIMARY KEY,
  street VARCHAR(255)
);

CREATE TABLE customers(
  id INT PRIMARY KEY,
  address INT,
  FOREIGN KEY (address) REFERENCES addresses(id)
);

INSERT INTO addresses VALUES (0, 'Astreet');
INSERT INTO addresses VALUES (1, 'Bstreet');
INSERT INTO customers VALUES (0, 0);
INSERT INTO customers VALUES (1, 0);
INSERT INTO customers VALUES (2, 1);
INSERT INTO customers VALUES (3, 1);
