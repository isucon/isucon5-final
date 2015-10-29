require 'sinatra/base'
require 'sinatra/contrib'
require 'mysql2-cs-bind'
require 'tilt/erubis'
require 'erubis'

require 'time'
require 'json'

$leader_board = nil
$leader_board_at = nil

$leader_history = nil
$leader_history_at = nil

$gcp_team_cache = {}

module Isucon5Portal
  class AuthenticationError < StandardError; end
end

require_relative 'lib/gcloud'

class Isucon5Portal::WebApp < Sinatra::Base
  set :erb, escape_html: true
  set :public_folder, File.expand_path('../public', __FILE__)
  set :sessions, true
  set :session_secret, ENV['ISUCON5_SESSION_SECRET'] || 'tony-moris'
  set :protection, true

  IN_PROCESS_CACHE_TIMEOUT = 30

  GAME_TIME =   [Time.parse("2015-10-31 11:00:00"), Time.parse("2015-10-31 18:00:00")]
  PUBLIC_TIME = [Time.parse("2015-10-31 11:00:00"), Time.parse("2015-10-31 17:45:00")]

  helpers do
    def config
      @config ||= {
        db: {
          host: ENV['ISUCON5_DB_HOST'] || 'localhost',
          port: ENV['ISUCON5_DB_PORT'] && ENV['ISUCON5_DB_PORT'].to_i,
          username: ENV['ISUCON5_DB_USER'] || 'root',
          password: ENV['ISUCON5_DB_PASSWORD'] || '',
          database: ENV['ISUCON5_DB_NAME'] || 'isucon5fportal',
        },
      }
    end

    def db
      return Thread.current[:isucon5_db] if Thread.current[:isucon5_db]
      client = Mysql2::Client.new(
        host: config[:db][:host],
        port: config[:db][:port],
        username: config[:db][:username],
        password: config[:db][:password],
        database: config[:db][:database],
        reconnect: true,
      )
      client.query_options.merge!(symbolize_keys: true)
      Thread.current[:isucon5_db] = client
      client
    end

    def is_organizer?(team)
      team[:priv] == 0
    end

    def is_guest?(team)
      team[:priv] == 2
    end

    def in_game?(team)
      now = Time.now
      case team[:priv]
      when 0 then true
      when 1 then GAME_TIME.first <= now && now < GAME_TIME.last
      when 2 then GAME_TIME.first <= now && now < GAME_TIME.last
      end
    end

    def in_public?(team)
      now = Time.now
      case team[:priv]
      when 0 then true
      when 1 then PUBLIC_TIME.first <= now && now < PUBLIC_TIME.last
      when 2 then PUBLIC_TIME.first <= now && now < PUBLIC_TIME.last
      end
    end

    def active_team?(team)
      now = Time.now
      case team[:priv]
      when 1 then GAME_TIME.first <= now && now < GAME_TIME.last
      when 2 then false
      when 0 then true
      end
    end

    def authenticate(account, password)
      query = <<SQL
SELECT * FROM teams WHERE account=? AND password=?
SQL
      result = db.xquery(query, account, password).first
      unless result
        raise Isucon5Portal::AuthenticationError
      end
      unless in_game?(team)
        raise Isucon5Portal::AuthenticationError
      end
      session[:team_id] = result[:id]
      result
    end

    def current_team
      if @team && active_team?(@team)
        return @team
      end

      return nil unless session[:team_id]

      @team = db.xquery('SELECT * FROM teams WHERE id=?', session[:team_id]).first
      unless @team
        session.clear
        raise Isucon5Portal::AuthenticationError
      end
      unless in_game?(@team)
        session.clear
        raise Isucon5Portal::AuthenticationError
      end
      @team[:ipaddresses] = JSON.parse(@team[:ipaddrs])
      @team
    end

    def authenticated!
      unless current_team
        redirect '/login'
      end
    end
  end

  error Isucon5Portal::AuthenticationError do
    session.clear
    halt 401, erb(:login, locals: {team_id: nil})
  end

  get '/login' do
    session.clear
    erb :login, locals: {team_id: nil}
  end

  post '/login' do
    authenticate params['account'], params['password']
    redirect '/'
  end

  get '/' do
    authenticated!
    team = current_team()
    erb :index, locals: {enable_actions: true, team_id: team[:id], team_name: team[:team], guest_priv: is_guest?(team), super_priv: is_organizer?(team)}
  end

  get '/messages' do
    authenticated!
    ary = []
    db.query('SELECT * FROM messages WHERE show_at < CURRENT_TIMESTAMP() AND CURRENT_TIMESTAMP() < hide_at ORDER BY show_at').each do |result|
      ary << { message: result[:content], priority: result[:priority] }
    end
    json ary
  end

  get '/team' do
    authenticated!
    halt 403 if is_guest?(curren_team)
    team = current_team()
    data = {
      enable_actions: false,
      team_id: team[:id],
      team: team[:team],
      account: team[:account],
      destination: team[:destination],
      ipaddrs: team[:ipaddrs],
    }
    erb :team, locals: data
  end

  post '/team' do
    authenticated!
    halt 403 if is_guest?(curren_team)
    query = <<SQL
UPDATE teams SET destination=? WHERE id=?
SQL
    db.xquery(query, params[:destination].strip, session[:team_id])
    redirect '/'
  end

  post '/enqueue' do
    authenticated!
    halt 403 if is_guest?(curren_team)

    team = current_team()

    if ! in_game?(team) && ! is_organizer?(team)
      return json({valid: false, message: "開始時刻まで待ってネ"})
    end

    ####################
    # TODO: for organizer enqueuing
    query = "SELECT COUNT(1) AS c FROM queue WHERE team_id=? AND status IN ('waiting','running')"
    existing = db.xquery(query, current_team[:id]).first[:c]
    if existing > 0 && team[:round] != 0
      return json({valid: false, message: "既にリクエスト済みです"})
    end

    # TODO: see team[:destination]
    ip_address = params[:ip_address]
    if ip_address.nil? || ip_address.empty?
      return json({valid: false, message: "IPアドレスが取得できません"})
    end
    unless ip_address =~ /^(\d+)\.(\d+)\.(\d+)\.(\d+)$/ && $1.to_i < 256 && $2.to_i < 256 && $3.to_i < 256 && $4.to_i < 256
      return json({valid: false, message: "IPアドレスを入力してください"})
    end

    testset_ids = db.xquery("SELECT id FROM testsets").map{|obj| obj[:id]}
    testset_id = testset_ids[rand(testset_ids.size)]

    db.xquery("INSERT INTO queue (team_id,status,ip_address,testset_id) VALUES (?,'waiting',?,?)", team[:id], ip_address, testset_id)
    ####################

    json({valid: true, message: "ベンチマークリクエストをキューに投入しました"})
  end

  get '/bench_detail/:id' do
    authenticated!
    halt 403 if is_guest?(curren_team)
    query = "SELECT id, team_id, summary, score, submitted_at, json FROM scores WHERE id=? AND team_id=?"
    data = db.xquery(query, params[:id], current_team[:id]).first()
    detail = JSON.parse(data[:json]) rescue nil

    unless data && detail
      return json({message: "スコア詳細取得に失敗しました(ID:#{params[:id]}, TEAM:#{current_team[:id]})"})
    end

    json({message: "SCORE: #{data[:score]}, RESULT: #{data[:summary]=="success" ? '成功' : '失敗'}", detail: detail})
  end

  get '/history' do
    authenticated!
    halt 403 if is_guest?(curren_team)

    query = "SELECT id, summary, score, submitted_at FROM scores WHERE team_id = ? ORDER BY submitted_at DESC"
    json(db.xquery(query, current_team[:id]).map{|row| { id: row[:id], team_id: current_team[:id], success: (row[:summary] == 'success'), score: row[:score], submitted_at: row[:submitted_at].strftime("%H:%M:%S") } })
  end

  get '/queuestat' do
    authenticated!
    # entire queue waiting/running list
    query = <<SQL
SELECT t.id AS team_id, t.team AS team, q.status AS status, q.acked_at AS acked_at
FROM queue q
JOIN teams t ON q.team_id = t.id
WHERE q.status IN ('waiting','running')
ORDER BY q.id
SQL
    json(db.xquery(query).map{|row| {team_id: row[:team_id], team_id_s: sprintf("%03d", row[:team_id]), team: row[:team], status: row[:status], acked_at: row[:acked_at]} })
  end

  get '/leader_board' do
    authenticated!

    if $leader_board && $leader_board_at && Time.now < $leader_board_at + IN_PROCESS_CACHE_TIMEOUT
      return json($leader_board)
    end

    ##########


    current_round = in_game_round_number(current_team)

    team_scores = {}

    team_scores_query = <<SQL
SELECT s.team_id AS team_id, t.team AS team_name, s.score AS score, s.submitted_at AS submitted_at
FROM scores s
JOIN teams t ON s.team_id = t.id
WHERE t.round = ? AND s.summary = 'success'
SQL
    db.xquery(team_scores_query, current_round).each do |row|
      team_id = row[:team_id]
      # SATURDAY_GAMETIME = [Time.parse("2015-09-26 11:00:00"), Time.parse("2015-09-26 19:00:00")]
      # SUNDAY_GAMETIME   = [Time.parse("2015-09-27 10:00:00"), Time.parse("2015-09-27 18:00:00")]
      gametime_end = (current_round == 1 ? SATURDAY_GAMETIME.last : SUNDAY_GAMETIME.last)
      if gametime_end < row[:submitted_at]
        next
      end

      if !team_scores.has_key?(team_id)
        team_scores[team_id] = {
          team_id: team_id, team_name: row[:team_name], shortname: row[:team_name][0..20], latest: row[:score], latest_at: row[:submitted_at]
        }
      elsif team_scores[team_id][:latest_at] < row[:submitted_at]
        team_scores[team_id][:latest] = row[:score]
        team_scores[team_id][:latest_at] = row[:submitted_at]
      end
    end

    high_score_teams_query = <<SQL
SELECT s.team_id AS team_id, t.team AS team_name, score AS highscore FROM highscores s JOIN teams t ON s.team_id = t.id
WHERE t.round = ?
SQL
    db.xquery(high_score_teams_query, current_round).each do |row|
      if team_scores[row[:team_id]]
        team_scores[row[:team_id]][:best] = row[:highscore]
      else
        p "something goes wrong: highscore exists, but latest doesn't exist: #{row[:team_id]}"
      end
    end

    if team_scores.size() < 1
      return json([])
    end

    top20 = team_scores.values.sort{|a,b| b[:latest] <=> a[:latest] }[0..20] # bigger than ealier

    $leader_board = top20
    $leader_board_at = Time.now

    json(top20)
  end

  get '/leader_history' do
    authorized!
    raise NotImplementedError
  end
end
