require 'sinatra/base'
require 'sinatra/contrib'
require 'mysql2-cs-bind'
require 'tilt/erubis'
require 'erubis'

require 'time'
require 'json'
require 'ipaddr'

$leader_board = nil
$leader_board_at = nil

$leader_history = nil
$leader_history_at = nil

module Isucon5Portal
  class AuthenticationError < StandardError; end
end

class Isucon5Portal::WebApp < Sinatra::Base
  set :erb, escape_html: true
  set :public_folder, File.expand_path('../public', __FILE__)
  set :sessions, true
  set :session_secret, ENV['ISUCON5_SESSION_SECRET'] || 'tony-moris'
  set :protection, true

  IN_PROCESS_CACHE_TIMEOUT = 30

  GAME_TIME =   [Time.parse("2015-10-31 11:00:00"), Time.parse("2015-10-31 18:00:00")]
  PUBLIC_TIME = [Time.parse("2015-10-31 11:00:00"), Time.parse("2015-10-31 17:45:00")]
  MARK_TIME =   [Time.parse("2015-10-31 18:15:00"), Time.parse("2015-10-31 20:00:00")]

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

    def in_mark_time?
      now = Time.now
      MARK_TIME.first <= now && now <= MARK_TIME.last
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
      else false
      end
    end

    def in_public?(team)
      now = Time.now
      case team[:priv]
      when 0 then true
      when 1 then PUBLIC_TIME.first <= now && now < PUBLIC_TIME.last
      when 2 then PUBLIC_TIME.first <= now && now < PUBLIC_TIME.last
      else false
      end
    end

    def active_team?(team)
      now = Time.now
      case team[:priv]
      when 0 then true
      when 1 then GAME_TIME.first <= now && now < GAME_TIME.last
      when 2 then GAME_TIME.first <= now && now < GAME_TIME.last
      else false
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
      unless in_game?(result)
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
    halt 403 if is_guest?(current_team)
    team = current_team()
    data = {
      enable_actions: false,
      team_id: team[:id],
      team_name: team[:team],
      team: team[:team],
      account: team[:account],
      destination: team[:destination],
      ipaddrlist: team[:ipaddresses].values,
      ipaddrs: team[:ipaddrs],
      guest_priv: is_guest?(team)
    }
    erb :team, locals: data
  end

  post '/team' do
    authenticated!
    halt 403 if is_guest?(current_team)

    query = <<SQL
UPDATE teams SET destination=? WHERE id=?
SQL
    db.xquery(query, params[:destination].strip, session[:team_id])
    redirect '/'
  end

  post '/enqueue' do
    authenticated!
    halt 403 if is_guest?(current_team)

    team = current_team()

    if ! in_game?(team) && ! is_organizer?(team)
      return json({valid: false, message: "開始時刻まで待ってネ"})
    end

    unless is_organizer?(team)
      check_existing_query = "SELECT COUNT(1) AS c FROM queue WHERE team_id=? AND status IN ('waiting','running')"
      existing = db.xquery(check_existing_query, team[:id]).first[:c]
      if existing > 0
        return json({valid: false, message: "既にリクエスト済みです"})
      end
    end

    team_id = team[:id]
    ip_address = params[:ip_address]

    if ip_address.nil? || ip_address.empty?
      dest_ip_account = team[:account]
      if is_organizer?(team) && params[:account]
        dest_ip_account = params[:account]
      end

      row = db.xquery("SELECT id,destination FROM teams WHERE account=?", dest_ip_account).first
      ip_address = row[:destination]
      team_id = row[:id]
    end

    if ip_address.nil? || ip_address.empty?
      return json({valid: false, message: "IPアドレスが取得できません"})
    elsif (IPAddr.new(ip_address) rescue nil).nil?
      return json({valid: false, message: "正しいIPアドレスを入力してください"})
    end

    testset_ids = db.xquery("SELECT id FROM testsets ORDER BY id").map{|obj| obj[:id]}
    testset_id = if in_mark_time?
                   testset_ids.last # (-1)
                 else
                   testset_ids[rand(testset_ids.size - 1)] # [0, -1)
                 end

    begin
      db.xquery("BEGIN")
      insert_query = "INSERT INTO queue (team_id,benchgroup,status,ip_address,testset_id) VALUES (?,?,'waiting',?,?)"
      db.xquery(insert_query, team_id, team[:benchgroup], ip_address, testset_id)
      num = db.xquery("SELECT COUNT(1) AS c FROM queue WHERE team_id=? AND status IN ('waiting','running')", team_id).first[:c]
      raise "already enqueued" if num > 1
      db.xquery("COMMIT")
    rescue => e
      db.xquery("ROLLBACK")
      if e.message == "already enqueued"
        return json({valid: false, message: "重複リクエストになったためキャンセルしました"})
      else
        return json({valid: false, message: "ベンチマークリクエスト登録時エラー: #{e.class}"})
      end
    end

    json({valid: true, message: "ベンチマークリクエストをキューに投入しました"})
  end

  get '/bench_detail/:id' do
    authenticated!
    halt 403 if is_guest?(current_team)
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
    halt 403 if is_guest?(current_team)

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

    teams = [] # [ {team: "..",  "best": 120, latest_at: "...", latest_summary: "fail", "latest": 100}, ...]
    all_teams_query = <<SQL
SELECT t.id AS id, t.team AS team, h.score AS best
FROM teams t
LEFT OUTER JOIN highscores h ON t.id = h.team_id
WHERE t.priv = 1
SQL
    team_query = <<SQL
SELECT summary, score, submitted_at FROM scores
WHERE team_id=? AND submitted_at >= ? AND submitted_at < ?
ORDER BY submitted_at DESC LIMIT 1
SQL
    all_teams = db.xquery(all_teams_query).to_a
    all_teams.each do |row|
      p1, p2 = (in_mark_time? ? MARK_TIME : PUBLIC_TIME)
      latest = db.xquery(team_query, row[:id], p1, p2).first || {}
      entry = {
        team: row[:team],
        best: row[:best].to_i || 0,
        latest_at: latest[:submitted_at],
        latest_summary: latest[:summary],
        latest: latest[:score].to_i || 0,
      }
      teams << entry
    end

    if teams.size() < 1
      $leader_board = []
      $leader_board_at = Time.now
      return json([])
    end

    list = if in_mark_time? # sort by latest
             teams.sort{|t1,t2| t2[:latest] <=> t1[:latest] }
           else # sort by best
             teams.sort{|t1,t2| (t2[:best] <=> t1[:best]).nonzero? || t1[:latest_at] <=> t2[:latest_at] }
           end
    $leader_board = list
    $leader_board_at = Time.now

    json(list)
  end

  get '/leader_history' do
    authenticated!

    if $leader_history && $leader_history_at && Time.now < $leader_history_at + IN_PROCESS_CACHE_TIMEOUT
      return json($leader_history)
    end

    teams = [] # [ {team: "..",  scores: [[1446258157,2000],[1446261757,3000], ...]}, ... ]
    all_teams_query = <<SQL
SELECT t.id AS id, t.team AS team
FROM teams t
WHERE t.priv = 1
SQL
    team_query = <<SQL
SELECT score, submitted_at FROM scores
WHERE team_id=? AND submitted_at >= ? AND submitted_at < ? AND summary = 'success'
ORDER BY submitted_at ASC
SQL
    all_teams = db.xquery(all_teams_query).to_a
    all_teams.each do |row|
      p1, p2 = (in_mark_time? ? MARK_TIME : PUBLIC_TIME)
      scores = db.xquery(team_query, row[:id], p1, p2).map do |score|
        [score[:submitted_at].to_i, score[:score]]
      end
      teams << { name: row[:team], data: scores }
    end

    if teams.size() < 1
      $leader_history = []
      $leader_history_at = Time.now
      return json([])
    end

    $leader_history = teams
    $leader_history_at = Time.now

    json(teams)
  end
end
