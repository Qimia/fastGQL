DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS addresses CASCADE;
DROP TABLE IF EXISTS streets CASCADE;

CREATE TABLE streets(
  id INT PRIMARY KEY
);

CREATE TABLE addresses(
  id INT PRIMARY KEY,
  street INT,
  FOREIGN KEY (street) REFERENCES streets(id)
);

CREATE TABLE customers(
  id INT PRIMARY KEY,
  address INT,
  FOREIGN KEY (address) REFERENCES addresses(id),
  street INT,
  FOREIGN KEY (street) REFERENCES streets(id)
);

INSERT INTO streets VALUES (0);
INSERT INTO streets VALUES (1);
INSERT INTO addresses VALUES (0, 0);
INSERT INTO addresses VALUES (1, 1);
INSERT INTO customers VALUES (0, 0, 0);
INSERT INTO customers VALUES (1, 1, 1);

