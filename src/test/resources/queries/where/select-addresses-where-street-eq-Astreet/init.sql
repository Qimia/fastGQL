DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS addresses CASCADE;

CREATE TABLE addresses(
  id INT PRIMARY KEY,
  street VARCHAR(255)
);

INSERT INTO addresses VALUES (0, 'Astreet');
INSERT INTO addresses VALUES (1, 'Bstreet');
INSERT INTO addresses VALUES (2, 'Cstreet');
