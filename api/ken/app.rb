require 'csv'

csvfile = File.expand_path("../x-ken-all.utf8.csv", __FILE__)

data = {} # num => [ data1, data2, ... ]
# data is [ "北海道 札幌市 中央区旭ケ丘" ]

# 13110,"153  ","1530042","ﾄｳｷｮｳﾄ","ﾒｸﾞﾛｸ","ｱｵﾊﾞﾀﾞｲ","東京都","目黒区","青葉台",0,0,1,0,0,0
CSV.foreach(csvfile) do |row|
  zipcode = row[2]
  address = "#{row[6]} #{row[7]} #{row[8]}"
  data[zipcode] ||= []
  data[zipcode] << address
end

require 'sinatra'
require 'sinatra/contrib'

get '/' do
  addrs = data[params['zipcode']]
  if addrs
    json({addresses: addrs})
  else
    json({addresses: []})
  end
end

get '/:zipcode' do
  addrs = data[params['zipcode']]
  if addrs
    json({addresses: addrs})
  else
    json({addresses: []})
  end
end
