#!/usr/bin/env ruby

require 'csv'
require 'yajl'

KEN_ALL_CSV = File.expand_path("../../api/ken/x-ken-all.utf8.csv", __FILE__)
DESTINATION = File.expand_path("../ken_all.json", __FILE__)

data = {}

CSV.foreach(KEN_ALL_CSV) do |row|
  zipcode = row[2]
  address = "#{row[6]} #{row[7]} #{row[8]}"
  data[zipcode] ||= []
  data[zipcode] << address
end

open(DESTINATION, 'w') do |io|
  Yajl::Encoder.encode(data, io)
end
