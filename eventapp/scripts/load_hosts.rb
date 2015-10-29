#!/usr/bin/env ruby

require 'json'

data = {}
(1..28).each do |n|
  num = sprintf("%02d", n)
  data[num] = {}
end

File.open("./hosts.txt") do |f|
  f.readlines.each do |line|
    ipaddr, hostname = line.chomp.split(/\s+/)
    hostname =~ /isu(\d{2})[abc]/
    team_num = $1
    data[team_num][hostname] = ipaddr
  end
end

data.keys.each do |key|
  puts key + "\t" + data[key].to_json
end
