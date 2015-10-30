#!/usr/bin/env ruby

require 'digest'

require 'faker'
require 'yajl'

srand(941)

CREATE_SQL_FILE = File.expand_path("../initialize.sql", __FILE__)
CREATE_JSON_FILE = File.expand_path("../source.json", __FILE__)

GENERATE_USERS = 10000
BENCH_CHUNKS_NUM = 20

GENERATE_GRADE_LIST = (
  [:micro] * (GENERATE_USERS * 0.4) + [:small] * (GENERATE_USERS * 0.3) +
  [:standard] * (GENERATE_USERS * 0.2) + [:premium] * (GENERATE_USERS * 0.1)
).shuffle

SOURCE_KEN_ALL = File.expand_path("../ken_all.json", __FILE__)
SOURCE_SURNAME = File.expand_path("../../api/search-name/surname_queries_response.json", __FILE__)
SOURCE_GIVENNAME = File.expand_path("../../api/search-name/givenname_queries_response.json", __FILE__)

KEN_ALL = File.open(SOURCE_KEN_ALL, 'r'){|f| Yajl::Parser.new.parse(f) }
ZIPCODE_LIST = KEN_ALL.keys
def zipcode
  ZIPCODE_LIST[rand(ZIPCODE_LIST.size)]
end

SURNAME = File.open(SOURCE_SURNAME, 'r'){|f| Yajl::Parser.new.parse(f) }
SURNAME_QUERIES = SURNAME.keys
def surname_query
  SURNAME_QUERIES[rand(SURNAME_QUERIES.size)]
end

GIVENNAME = File.open(SOURCE_GIVENNAME, 'r'){|f| Yajl::Parser.new.parse(f) }
GIVENNAME_QUERIES = GIVENNAME.keys
def givenname_query
  GIVENNAME_QUERIES[rand(GIVENNAME_QUERIES.size)]
end

PERFECT_SEC_REQ_LIST = ["perfect", "ultimate", "exorbitant", "extreme", "supreme", "abnormal", "magnificent", "unforgettable"]
def perfectsec_req
  PERFECT_SEC_REQ_LIST[rand(PERFECT_SEC_REQ_LIST.size)]
end
def perfectsec_token
  Digest::SHA1.hexdigest("tony" + rand(1000000).to_s)
end

def create_user_data(user_id, grade)
  user_name = Faker::Internet.user_name
  user = {
    id: user_id,
    email: "#{user_name}#{user_id}@isucon.net",
    salt: Faker::Internet.password(8),
    password: "#{user_name}#{user_id}",
    grade: grade.to_s,
  }
  user_tenki = zipcode()
  sub = {
    ken: { keys: [ user_tenki ] },
    ken2: { params: { zipcode: zipcode() } },
    surname: { params: { q: surname_query() } },
  }
  if [:small, :standard, :premium].include? grade
    sub[:givenname] = { params: { q: givenname_query() } }
  end
  if [:standard, :premium].include? grade
    sub[:tenki] = { token: user_tenki }
  end
  if [:premium].include? grade
    user_perfectsec_token = perfectsec_token()
    sub[:perfectsec] = {
      params: { req: perfectsec_req() },
      token: user_perfectsec_token,
    }
    sub[:perfectsec_attacked] = { token: user_perfectsec_token }
  end
  user[:sub] = sub
  user
end

# CREATE TYPE grades AS ENUM ('micro', 'small', 'standard', 'premium');
# CREATE TABLE users (
#   id SERIAL PRIMARY KEY,
#   email VARCHAR(256) NOT NULL,
#   salt VARCHAR(32) NOT NULL,
#   passhash bytea NOT NULL,
#   grade grades
# );

# CREATE TYPE token_types AS ENUM ('header', 'param');
# CREATE TABLE endpoints (
#   service VARCHAR(32) NOT NULL PRIMARY KEY,
#   meth VARCHAR(16) NOT NULL,
#   token_type token_types,
#   token_key VARCHAR(64),
#   uri TEXT
# );

# CREATE TABLE subscriptions (
#   user_id INTEGER REFERENCES users (id) NOT NULL PRIMARY KEY,
#   arg TEXT
# );

sql = File.new(CREATE_SQL_FILE, 'w')

sql.write <<SQL
TRUNCATE endpoints;

INSERT INTO endpoints (service, meth, token_type, token_key, uri)
VALUES
('ken', 'GET', NULL, NULL, 'http://api.five-final.isucon.net:8080/%s'),
('ken2', 'GET', NULL, NULL, 'http://api.five-final.isucon.net:8080/'),
('surname', 'GET', NULL, NULL, 'http://api.five-final.isucon.net:8081/surname'),
('givenname', 'GET', NULL, NULL, 'http://api.five-final.isucon.net:8081/givenname'),
('tenki', 'GET', 'param', 'zipcode', 'http://api.five-final.isucon.net:8988/'),
('perfectsec', 'GET', 'header', 'X-PERFECT-SECURITY-TOKEN', 'https://api.five-final.isucon.net:8443/tokens'),
('perfectsec_attacked', 'GET', 'header', 'X-PERFECT-SECURITY-TOKEN', 'https://api.five-final.isucon.net:8443/attacked_list');

TRUNCATE users, subscriptions;

INSERT INTO users (id, email, salt, passhash, grade) VALUES
SQL

users = []

# (1, 'moris@tagomor.is',  '111111', digest('111111' || 'moris', 'sha512'), 'premium')
GENERATE_GRADE_LIST.each_with_index do |grade, index|
  user = create_user_data(index + 1, grade)
  users << user
  tailing = index == (GENERATE_GRADE_LIST.size - 1) ? ";" : ","
  line = <<EOL
(#{user[:id]}, '#{user[:email]}', '#{user[:salt]}', digest('#{user[:salt]}' || '#{user[:password]}', 'sha512'), '#{user[:grade]}')#{tailing}
EOL
  sql.write line
end

sql.write <<SQL

SELECT SETVAL('users_id_seq', #{users.size});

INSERT INTO subscriptions (user_id, arg) VALUES
SQL

# (1, '{"ken":{"keys":["6900014"]},"ken2":{"params":{"zipcode":"1530042"}},"surname":{"params":{"q":"神"}},"givenname":{"params":{"q":"さと"}},"tenki":{"token":"0100001"},"perfectsec":{"params":{"req":"ps1"},"token":"da39a3ee5e6b4b0d3255bfef95601890afd80709"},"perfectsec_attacked":{"token":"da39a3ee5e6b4b0d3255bfef95601890afd80709"}}')
users.each_with_index do |user, index|
  arg = Yajl::Encoder.encode(user[:sub])
  tailing = index == (users.size - 1) ? ";" : ","
  line = <<EOL
(#{user[:id]}, '#{arg}')#{tailing}
EOL
  sql.write line
end

sql.close

data = []
BENCH_CHUNKS_NUM.times do
  data << []
end
users.shuffle.each do |user|
  data.first << {email: user[:email], password: user[:password], grade: user[:grade], subscriptions: user[:sub]}
  data.rotate!
end

File.open(CREATE_JSON_FILE, 'w') do |f|
  Yajl::Encoder.encode(data, f)
end
