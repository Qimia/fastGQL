DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS addresses CASCADE;

CREATE TABLE addresses(
  id INT PRIMARY KEY
);

INSERT INTO addresses VALUES (0);
