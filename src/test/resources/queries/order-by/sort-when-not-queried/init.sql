DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS addresses CASCADE;
DROP TABLE IF EXISTS streets CASCADE;

CREATE TABLE addresses(
  id INT PRIMARY KEY,
  description VARCHAR(255)
);

INSERT INTO addresses VALUES (0, 'zero');
INSERT INTO addresses VALUES (1, 'one');
INSERT INTO addresses VALUES (2, 'two');
