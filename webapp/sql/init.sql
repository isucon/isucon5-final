-- cat init.sql | psql postgres
DROP DATABASE isucon5f;
DROP USER isucon;

CREATE USER isucon;

CREATE DATABASE isucon5f OWNER isucon ENCODING 'utf8';

\connect isucon5f

CREATE TYPE grades AS ENUM ('micro', 'small', 'standard', 'premium');

CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  email VARCHAR(256) NOT NULL,
  salt VARCHAR(32) NOT NULL,
  passhash bytea NOT NULL, -- SHA2 512
  grade grades
);

CREATE EXTENSION pgcrypto;

INSERT INTO users (email, salt, passhash, grade) VALUES
('moris@tagomor.is',  '111111', digest('111111' || 'moris', 'sha512'), 'premium'), -- 1
('kamipo@tagomor.is', '111112', digest('111112' || 'kamipo', 'sha512'), 'micro'), -- 2
('941@tagomor.is',    '111113', digest('111113' || '941', 'sha512'), 'small'), -- ..
('making@tagomor.is', '111114', digest('111114' || 'making', 'sha512'), 'standard'),
('najeira@tagomor.is', '111115', digest('111115' || 'najeira', 'sha512'), 'premium'),
('hydrakecat@tagomor.is', '111116', digest('111116' || 'hydrakecat', 'sha512'), 'micro'),
('taroleo@tagomor.is', '111117', digest('111117' || 'taroleo', 'sha512'), 'small'),
('hokaccha@tagomor.is', '111118', digest('111118' || 'hokaccha', 'sha512'), 'standard');

CREATE TYPE token_types AS ENUM ('header', 'param');

CREATE TABLE endpoints (
  service VARCHAR(32) NOT NULL PRIMARY KEY,
  meth VARCHAR(16) NOT NULL,
  token_type token_types,
  token_key VARCHAR(64),
  uri TEXT -- http://127.0.0.1:8080/%s
);

INSERT INTO endpoints (service, meth, token_type, token_key, uri)
VALUES
('ken', 'GET', NULL, NULL, 'http://127.0.0.1:8080/%s'),
('ken2', 'GET', NULL, NULL, 'http://127.0.0.1:8080/');

CREATE TABLE subscriptions (
  user_id INTEGER REFERENCES users (id) NOT NULL PRIMARY KEY,
  arg TEXT -- json
);

INSERT INTO subscriptions (user_id, arg)
VALUES
(1, '{"ken":{"keys":["6900014"]},"ken2":{"params":{"zipcode":"1530042"}}}'),
(2, '{"ken":{"keys":["6900014"]},"ken2":{"params":{"zipcode":"1530042"}}}')
;
