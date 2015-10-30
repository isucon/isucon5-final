<?php
require 'vendor/autoload.php';

date_default_timezone_set('Asia/Tokyo');
mb_internal_encoding('UTF-8');

$app = new \Slim\Slim(array(
    'db' => array(
        'host' => getenv('ISUCON5_DB_HOST') ?: 'localhost',
        'port' => (int)getenv('ISUCON5_DB_PORT') ?: 5432,
        'username' => getenv('ISUCON5_DB_USER') ?: 'isucon',
        'password' => getenv('ISUCON5_DB_PASSWORD'),
        'database' => getenv('ISUCON5_DB_NAME') ?: 'isucon5f'
    ),
    'cookies.encrypt' => true,
));

$app->add(new \Slim\Middleware\SessionCookie(array(
    'secret' => getenv('ISUCON5_SESSION_SECRET') ?: 'tonymoris',
    'expires' => 0,
)));

function h($string)
{
    echo htmlspecialchars($string, ENT_QUOTES, 'UTF-8');
}

function db()
{
    global $app;
    static $db;
    if (!$db) {
        $config = $app->config('db');
        $dsn = sprintf("pgsql:host=%s;port=%s;dbname=%s", $config['host'], $config['port'], $config['database']);
        $options = array(
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        );
        $db = new PDO($dsn, $config['username'], $config['password'], $options);
    }
    return $db;
}

function db_execute($query, $args = array())
{
    $stmt = db()->prepare($query);
    $stmt->execute($args);
    return $stmt;
}

function authenticate($email, $password)
{
    $query = <<<SQL
SELECT id, email, grade FROM users WHERE email=? AND passhash=digest(salt || ?, 'sha512')
SQL;
    $user = db_execute($query, array($email, $password))->fetch();
    if ($user) {
        $_SESSION['user_id'] = $user['id'];
    }
    return $user;
}

function current_user()
{
    static $user;
    if ($user) return $user;
    if (!isset($_SESSION['user_id'])) return null;
    $user = db_execute('SELECT id,email,grade FROM users WHERE id=?', array($_SESSION['user_id']))->fetch();
    if (!$user) {
        $_SESSION = array();
    }
    return $user;
}

$SALT_CHARS = array_merge(range('a', 'z'), range('A', 'Z'), range('0', '9'));
function generate_salt()
{
    global $SALT_CHARS;
    $salt = '';
    foreach (range(1, 32) as $n) {
        $salt .= $SALT_CHARS[rand(0, count($SALT_CHARS)-1)];
    }
    return $salt;
}

$app->get('/signup', function () use ($app) {
    $_SESSION = array();
    $app->render('signup.php');
});

$app->post('/signup', function () use ($app) {
    $params = $app->request->params();
    $email = $params['email'];
    $password = $params['password'];
    $grade = $params['grade'];
    $salt = generate_salt();
    $insert_user_query = <<<SQL
INSERT INTO users (email,salt,passhash,grade) VALUES (?,?,digest(? || ?, 'sha512'),?) RETURNING id
SQL;
    $default_arg = array();
    $insert_subscription_query = <<<SQL
INSERT INTO subscriptions (user_id,arg) VALUES (?,?)
SQL;
    db()->beginTransaction();
    $user_id = db_execute($insert_user_query, array($email, $salt, $salt, $password, $grade))->fetch()['id'];
    db_execute($insert_subscription_query, array($user_id, json_encode($default_arg, JSON_FORCE_OBJECT)));
    db()->commit();
    $app->redirect('/login');
});

$app->post('/cancel', function () use ($app) {
    $app->redirect('/signup');
});

$app->get('/login', function () use ($app) {
    $_SESSION = array();
    $app->render('login.php');
});

$app->post('/login', function () use ($app) {
    $params = $app->request->params();
    authenticate($params['email'], $params['password']);
    if (!current_user()) $app->halt(403);
    $app->redirect('/');
});

$app->get('/logout', function () use ($app) {
    $_SESSION = array();
    $app->redirect('/login');
});

$app->get('/', function () use ($app) {
    if (!current_user()) {
        return $app->redirect('/login');
    }
    $app->render('main.php', array('user' => current_user()));
});

$app->get('/user.js', function () use ($app) {
    if (!current_user()) $app->halt(403);
    $app->response->headers->set('Content-Type', 'application/javascript');
    $app->render('userjs.php', array('grade' => current_user()['grade']));
});

$app->get('/modify', function () use ($app) {
    $user = current_user();
    if (!$user) $app->halt(403);
    $query = <<<SQL
SELECT arg FROM subscriptions WHERE user_id=?
SQL;
    $arg = db_execute($query, array($user['id']))->fetch()['arg'];
    $app->render('modify.php', array('user' => $user, 'arg' => $arg));
});

$app->post('/modify', function () use ($app) {
    $user = current_user();
    if (!$user) $app->halt(403);
    $params = $app->request->params();
    $service = isset($params["service"]) ? trim($params["service"]) : null;
    $token = isset($params["token"]) ? trim($params["token"]) : null;
    $keys = isset($params["keys"]) ? preg_split('/\s+/', trim($params["keys"])) : null;
    $param_name = isset($params["param_name"]) ? trim($params["param_name"]) : null;
    $param_value = isset($params["param_value"]) ? trim($params["param_value"]) : null;
    $select_query = <<<SQL
SELECT arg FROM subscriptions WHERE user_id=? FOR UPDATE
SQL;
    $update_query = <<<SQL
UPDATE subscriptions SET arg=? WHERE user_id=?
SQL;
    db()->beginTransaction();
    $arg_json = db_execute($select_query, array($user['id']))->fetch()['arg'];
    $arg = json_decode($arg_json, true);
    if (!isset($arg[$service])) $arg[$service] = array();
    if ($token) $arg[$service]['token'] = $token;
    if ($keys) $arg[$service]['keys'] = $keys;
    if ($param_name && $param_value) {
        if (!isset($arg[$service]['params'])) $arg[$service]['params'] = array();
        $arg[$service]['params'][$param_name] = $param_value;
    }
    db_execute($update_query, array(json_encode($arg), $user['id']));
    db()->commit();
    $app->redirect('/modify');
});

function fetch_api($method, $uri, $headers, $params)
{
    $client = new GuzzleHttp\Client(['verify' => false]);
    $res = $client->request($method, $uri, [
        'headers' => $headers,
        'query' => $params,
    ]);
    return json_decode($res->getBody(), true);
}

$app->get('/data', function () use ($app) {
    $user = current_user();
    if (!$user) $app->halt(403);

    $arg_json = db_execute("SELECT arg FROM subscriptions WHERE user_id=?", array($user['id']))->fetch()['arg'];
    $arg = json_decode($arg_json, true);

    $data = array();

    foreach ($arg as $service => $conf) {
        $row = db_execute("SELECT meth, token_type, token_key, uri FROM endpoints WHERE service=?", array($service))->fetch();
        $method = $row['meth'];
        $token_type = $row['token_type'];
        $token_key = $row['token_key'];
        $uri_template = $row['uri'];
        $headers = array();
        $params = isset($conf['params']) ? $conf['params'] : array();
        switch ($token_type) {
        case 'header':
            $headers[$token_key] = $conf['token'];
            break;
        case 'param':
            $params[$token_key] = $conf['token'];
            break;
        }
        $args = isset($conf['keys']) ? $conf['keys'] : array();
        array_unshift($args, $uri_template);
        $uri = call_user_func_array('sprintf', $args);
        $data[] = array("service" => $service, "data" => fetch_api($method, $uri, $headers, $params));
    }

    $app->response->headers->set('Content-Type', 'application/json');
    $app->response->body(json_encode($data));
});

$app->get('/initialize', function () use ($app) {
    $file = realpath(dirname(__FILE__) . "/../sql/initialize.sql");
    exec("psql -f " . $file . " isucon5f");
});

$app->run();
