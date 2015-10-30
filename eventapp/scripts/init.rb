#!/usr/bin/env ruby

require 'mysql2-cs-bind'
require 'json'

client = Mysql2::Client.new(
  host: 'localhost',
  username: 'root',
  database: 'isucon5fportal'
  reconnect: true,
)
client.query_options.merge!(symbolize_keys: true)

# TODO: for first message
MESSAGES = [
  # { priority: 'alert-info', content: '', show_at: '', hide_at: '' },
]

MESSAGES.each do |msg|
  client.xquery(
    "INSERT INTO messages (priority,content,show_at,hide_at) VALUES (?,?,?,?)",
    msg[:priority], msg[:content], msg[:show_at], msg[:hide_at]
  )
end

TEAMS_QUERY = <<SQL
INSERT INTO teams (account,benchgroup,priv,team,password,ipaddrs) VALUES
('team01',1,1,'fujiwara組','mcsAnG6D','{"isu01a":"203.104.208.171","isu01b":"203.104.208.172","isu01c":"203.104.208.173"}'),
('team02',1,1,'GoBold','gFHE4Vxa','{"isu02a":"203.104.208.174","isu02b":"203.104.208.175","isu02c":"203.104.208.176"}'),
('team03',1,1,'.dat','YUsCAt1G','{"isu03a":"203.104.208.177","isu03b":"203.104.208.178","isu03c":"203.104.208.179"}'),
('team04',1,1,'chatzmers','eNzau8Yk','{"isu04a":"203.104.208.180","isu04b":"203.104.208.181","isu04c":"203.104.208.182"}'),
('team05',2,1,'はむちゃん','e7StJuU9','{"isu05a":"203.104.208.183","isu05b":"203.104.208.184","isu05c":"203.104.208.185"}'),
('team06',2,1,'2608','eY2TPwq4','{"isu06a":"203.104.208.186","isu06b":"203.104.208.187","isu06c":"203.104.208.188"}'),
('team07',2,1,'チーム hammer','gm8zANdf','{"isu07a":"203.104.208.189","isu07b":"203.104.208.190","isu07c":"203.104.208.191"}'),
('team08',2,1,'大森s','d6FdHeWC','{"isu08a":"203.104.208.192","isu08b":"203.104.208.193","isu08c":"203.104.208.194"}'),
('team09',3,1,'2nd Party Cookies','xExC0sHn','{"isu09a":"203.104.208.195","isu09b":"203.104.208.196","isu09c":"203.104.208.197"}'),
('team10',3,1,'にゃーん','2WhLeQBm','{"isu10a":"203.104.208.198","isu10b":"203.104.208.199","isu10c":"203.104.208.200"}'),
('team11',3,1,'チームフリー素材+α','XgyvbC4w','{"isu11a":"203.104.208.201","isu11b":"203.104.208.202","isu11c":"203.104.208.203"}'),
('team12',3,1,'白金動物園','dybM9u6C','{"isu12a":"203.104.208.204","isu12b":"203.104.208.205","isu12c":"203.104.208.206"}'),
('team13',4,1,'チーム学生自治','s3sYj9JP','{"isu13a":"203.104.208.207","isu13b":"203.104.208.208","isu13c":"203.104.208.209"}'),
('team14',4,1,'ピザはバランスいい','SWg4f8Kk','{"isu14a":"203.104.208.210","isu14b":"203.104.208.211","isu14c":"203.104.208.212"}'),
('team15',4,1,'マウント竹田氏','beWq6G57','{"isu15a":"203.104.208.213","isu15b":"203.104.208.214","isu15c":"203.104.208.215"}'),
('team16',4,1,'醤丸','ed6K78aJ','{"isu16a":"203.104.208.216","isu16b":"203.104.208.217","isu16c":"203.104.208.218"}'),
('team17',5,1,'lily white','58fRC7Sk','{"isu17a":"203.104.208.219","isu17b":"203.104.208.220","isu17c":"203.104.208.221"}'),
('team18',5,1,'ヴェンティッグ','4T09S7qT','{"isu18a":"203.104.208.222","isu18b":"203.104.208.223","isu18c":"203.104.208.224"}'),
('team19',5,1,'†空中庭園†《ガーデンプレイス》','na7fhRdn','{"isu19a":"203.104.208.225","isu19b":"203.104.208.226","isu19c":"203.104.208.227"}'),
('team20',5,1,'古典論理の犬','vswB704S','{"isu20a":"203.104.208.228","isu20b":"203.104.208.229","isu20c":"203.104.208.230"}'),
('team21',6,1,'へしこず','4Z4nEK0R','{"isu21a":"203.104.208.231","isu21b":"203.104.208.232","isu21c":"203.104.208.233"}'),
('team22',6,1,'negainoido','nm80yVzZ','{"isu22a":"203.104.208.234","isu22b":"203.104.208.235","isu22c":"203.104.208.236"}'),
('team23',6,1,'kstm','DShgwOd8','{"isu23a":"203.104.208.237","isu23b":"203.104.208.238","isu23c":"203.104.208.239"}'),
('team24',6,1,'maguro','GtC9FpYT','{"isu24a":"203.104.208.240","isu24b":"203.104.208.241","isu24c":"203.104.208.242"}'),
('team25',6,1,'アジ・タコ・エンガワ！','f29B4Twg','{"isu25a":"203.104.208.243","isu25b":"203.104.208.244","isu25c":"203.104.208.245"}'),
('team26',6,0,'運営そのいち','kamipo','{"isu26a":"203.104.208.246","isu26b":"203.104.208.247","isu26c":"203.104.208.248"}'),
('team27',6,0,'運営そのに','kamipo','{"isu27a":"203.104.208.249","isu27b":"203.104.208.250","isu27c":"203.104.208.251"}'),
('team28',6,0,'運営そのさん','kamipo','{"isu28a":"203.104.208.252","isu28b":"203.104.208.253","isu28c":"203.104.208.254"}'),
('guest',0,2,'観客アカウント','guest','{}');
SQL

client.query(TEAMS_QUERY)

testsets = File.open(File.expand_path("../../../data/source.json", __FILE__)){|f| JSON.parse(f.read) }
testsets.each do |set|
  client.xquery("INSERT INTO testsets (json) VALUES (?)", set.to_json)
end

client.xquery("SELECT account,password,team,ipaddrs FROM teams WHERE priv=1").each do |row|
  ipaddrs = []
  JSON.parse(row[:ipaddrs]).each_pair do |host,addr|
    ipaddrs << "#{host}: #{addr}"
  end
  line = <<EOL
"#{row[:account]}","#{row[:password]}","#{row[:team]}","#{ipaddrs[0]}","#{ipaddrs[1]}","#{ipaddrs[2]}"
EOL
  print line
end
