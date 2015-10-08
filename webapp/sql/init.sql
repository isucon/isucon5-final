DROP USER isucon;
DROP DATABASE isucon5f;

CREATE USER isucon;

CREATE DATABASE isucon5f OWNER isucon ENCODING 'utf8';

\connect isucon5f

CREATE TYPE grades AS ENUM ('micro', 'small', 'standard', 'premium');

CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  email VARCHAR(256) NOT NULL,
  passhash VARCHAR(128) NOT NULL, -- SHA2 512
  grade grades
);

CREATE EXTENSION pgcrypto;

INSERT INTO users (email, salt, passhash, grade)
VALUES ('moris@tagomor.is',  '111111', digest('111111' || 'moris', 'sha512'), 'premium'), -- 1
VALUES ('kamipo@tagomor.is', '111112', digest('111112' || 'kamipo', 'sha512'), 'micro'), -- 2
VALUES ('941@tagomor.is',    '111113', digest('111113' || '941', 'sha512'), 'small'), -- ..
VALUES ('making@tagomor.is', '111114', digest('111114' || 'making', 'sha512'), 'standard'),
VALUES ('najeira@tagomor.is', '111115', digest('111115' || 'najeira', 'sha512'), 'premium'),
VALUES ('hydrakecat@tagomor.is', '111116', digest('111116' || 'hydrakecat', 'sha512'), 'micro'),
VALUES ('taroleo@tagomor.is', '111117', digest('111117' || 'taroleo', 'sha512'), 'small'),
VALUES ('hokaccha@tagomor.is', '111118', digest('111118' || 'hokaccha', 'sha512'), 'standard');

CREATE TABLE endpoints (
  service VARCHAR(32) NOT NULL PRIMARY KEY,
  hostname VARCHAR(128) NOT NULL,
  port integer NOT NULL
);

INSERT INTO endpoints (service, hostname, port)
VALUES ('ken', 'localhost', 8080);

CREATE TABLE subscriptions (
  user_id INTEGER REFERENCES users (id),
  service VARCHAR(32) NOT NULL,
  token TEXT,
  argument TEXT -- json    
);

INSERT INTO subscriptions (user_id, service, argument)
VALUES (1, 'ken', '{"key":"6900014"}'),
VALUES (1, 'ken', '{"key":"1530042"}');
