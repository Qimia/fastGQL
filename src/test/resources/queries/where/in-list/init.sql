DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS addresses CASCADE;
DROP TABLE IF EXISTS streets CASCADE;

CREATE TABLE addresses(
  id INT PRIMARY KEY
);

INSERT INTO addresses VALUES (0);
INSERT INTO addresses VALUES (1);
INSERT INTO addresses VALUES (2);