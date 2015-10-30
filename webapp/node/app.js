'use strict';

const express = require('express');
const cookieSession = require('cookie-session');
const bodyParser = require('body-parser');
const pg = require('pg');
const async = require('async');
const execSync = require('child_process').execSync;
const util = require('util');
const path = require('path');
const request = require('request');
const _ = require('lodash');
const app = express();

app.set('view engine', 'ejs')
app.use(express.static(__dirname + '/../static'));
app.use(bodyParser.urlencoded({ extended: false }));

let config = {
  db: {
    host: process.env.ISUCON5_DB_HOST || 'localhost',
    port: process.env.ISUCON5_DB_PORT,
    user: process.env.ISUCON5_DB_USER || 'isucon',
    password: process.env.ISUCON5_DB_PASSWORD,
    database: process.env.ISUCON5_DB_NAME || 'isucon5f',
  },
};

function requireLogin(req, res, next) {
  if (req.currentUser) return next();

  req.done();
  res.sendStatus(403);
}

app.use(cookieSession({
  name: 'isucon5f',
  secret: process.env.ISUCON5_SESSION_SECRET || 'tonymoris',
}));

app.use((req, res, next) => {
  pg.connect(config.db, (err, client, done) => {
    if (err) return next(err);
    req.client = client;
    req.done = done;
    next();
  });
});

app.use((req, res, next) => {
  let userId = req.session.userId;
  if (!userId) return next();

  req.client.query('SELECT id,email,grade FROM users WHERE id=$1', [userId], (err, result) => {
    if (err) return next(err);
    let user = result.rows[0];
    if (!user) {
      req.session = null;
    }
    req.currentUser = user;
    next();
  });
});

app.get('/', (req, res, next) => {
  if (req.currentUser) {
    res.render('main', { user: req.currentUser });
  }
  else {
    res.redirect('/login');
  }
  req.done();
});

app.get('/user.js', requireLogin, (req, res, next) => {
  res.set('Content-Type', 'application/javascript');
  res.render('userjs', { grade: req.currentUser.grade });
  req.done();
});

app.get('/signup', (req, res, next) => {
  req.session = null;
  res.render('signup');
  req.done();
});

app.get('/login', (req, res, next) => {
  req.session = null;
  res.render('login');
  req.done();
});

app.post('/login', (req, res, next) => {
  let email = req.body.email;
  let password = req.body.password;
  let query = "SELECT id, email, grade FROM users WHERE email=$1 AND passhash=digest(salt || $2, 'sha512')";

  req.client.query(query, [email, password], (err, result) => {
    if (err) return next(err);

    let user = result.rows[0];
    req.done();
    if (user) {
      req.session.userId = user.id;
      res.redirect('/');
    }
    else {
      res.sendStatus(403);
    }
  });
});

app.get('/logout', (req, res, next) => {
  req.session = null;
  res.redirect('/login');
  req.done();
});

app.get('/signup', (req, res, next) => {
  req.session = null;
  res.render('signup');
  req.done();
});

const SALT_CHARS = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'.split('');
function generateSalt() {
  return _(32).times(() => _.sample(SALT_CHARS)).join('');
}

app.post('/signup', (req, res, next) => {
  let email = req.body.email;
  let password = req.body.password;
  let grade = req.body.grade;
  let salt = generateSalt();
  let insertUserQuery = "INSERT INTO users (email,salt,passhash,grade) VALUES ($1,$2,digest($3 || $4, 'sha512'),$5) RETURNING id";
  let defaultArg = {};
  let insertSubscriptionQuery = 'INSERT INTO subscriptions (user_id,arg) VALUES ($1,$2)';

  async.waterfall([
    (fn) => req.client.query('BEGIN', fn),
    (r, fn) => req.client.query(insertUserQuery, [email, salt, salt, password, grade], fn),
    (r, fn) => req.client.query(insertSubscriptionQuery, [r.rows[0].id, JSON.stringify(defaultArg)], fn),
    (r, fn) => req.client.query('COMMIT', fn),
  ], err => {
    if (err) {
      req.client.query('ROLLBACK', _err => next(err));
    }
    else {
      res.redirect('/login');
      req.done();
    }
  });
});

app.post('/cancel', (req, res, next) => {
  res.redirect('/signup');
  req.done();
});

app.get('/modify', requireLogin, (req, res, next) => {
  let query = 'SELECT arg FROM subscriptions WHERE user_id=$1 FOR UPDATE';
  req.client.query(query, [req.currentUser.id], (err, result) => {
    if (err) return next(err);
    res.render('modify', { user: req.currentUser, arg: result.rows[0].arg });
    req.done();
  });
});

app.post('/modify', requireLogin, (req, res, next) => {
  let service = req.body.service;
  let token = req.body.token === undefined ? null : req.body.token.trim();
  let keys = req.body.keys === undefined ? null : req.body.keys.trim().split(/\s+/);
  let paramName = req.body.param_name === undefined ? null : req.body.param_name.trim();
  let paramValue = req.body.param_value === undefined ? null : req.body.param_value.trim();
  let selectQuery = 'SELECT arg FROM subscriptions WHERE user_id=$1 FOR UPDATE';
  let updateQuery = 'UPDATE subscriptions SET arg=$1 WHERE user_id=$2';

  async.waterfall([
    (fn) => req.client.query('BEGIN', fn),
    (r, fn) => req.client.query(selectQuery, [req.currentUser.id], fn),
    (r, fn) => {
      let arg = JSON.parse(r.rows[0].arg);

      if (!arg[service]) {
        arg[service] = {};
      }
      if (token !== null) {
        arg[service].token = token
      }
      if (keys !== null) {
        arg[service].keys = keys
      }
      if (paramName !== null && paramValue !== null) {
        if (!arg[service]['params']) {
          arg[service]['params'] = {};
        }
        arg[service]['params'][paramName] = paramValue
      }

      fn(null, arg);
    },
    (arg, fn) => req.client.query(updateQuery, [JSON.stringify(arg), req.currentUser.id], fn),
    (r, fn) => req.client.query('COMMIT', fn),
  ], err => {
    if (err) {
      req.client.query('ROLLBACK', _err => next(err));
    }
    else {
      res.redirect('/modify');
      req.done();
    }
  });
});

function fetchApi(method, uri, headers, params, fn) {
  let options = { method, uri, headers, json: true };

  if (/^https:\/\//.test(uri)) {
    options.rejectUnauthorized = false;
  }

  if (method === 'GET') {
    options.qs = params;
  }
  else if (method === 'POST') {
    options.form = params;
  }
  else {
    fn(new Error(`unknown method ${method}`));
  }

  request(options, (err, response, body) => {
    fn(err, body);
  });
}

app.get('/data', requireLogin, (req, res, next) => {
  let argQuery = 'SELECT arg FROM subscriptions WHERE user_id=$1';
  let data = [];

  req.client.query(argQuery, [req.currentUser.id], (err, result) => {
    if (err) return next(err);

    let arg = JSON.parse(result.rows[0].arg);
    async.forEachOfSeries(arg, (conf, service, next) => {
      let query = "SELECT meth, token_type, token_key, uri FROM endpoints WHERE service=$1";
      req.client.query(query, [service], (err, result) => {
        if (err) return next(err);
        let row = result.rows[0];
        let method = row.meth;
        let tokenType = row.token_type;
        let tokenKey = row.token_key;
        let uriTemplate = row.uri;
        let headers = {};
        let params = Object.assign({}, conf.params);
        switch (tokenType) {
          case 'header':
            headers[tokenKey] = conf.token;
            break;
          case 'param':
            params[tokenKey] = conf.token;
            break;
        }
        let keys = conf.keys || [];
        let uri = util.format.apply(null, [uriTemplate].concat(keys))
        fetchApi(method, uri, headers, params, (err, res) => {
          if (err) return next(err);
          data.push({ service: service, data: res });
          next();
        });
      });
    }, err => {
      if (err) return next(err);
      res.send(data);
      req.done();
    });
  });
});

app.get('/initialize', (req, res, next) => {
  let file = path.normalize(__dirname + "/../sql/initialize.sql");
  execSync("psql -f " + file + " isucon5f")
  req.done();
  res.sendStatus(200);
});

app.use((req, res, next) => {
  res.sendStatus(404);
  req.done();
});

app.use((err, req, res, next) => {
  console.error(err.stack);
  req.done();
  res.sendStatus(500);
});

let server = app.listen(8080, () => {
  console.log(`listening at ${server.address().port}`);
});
