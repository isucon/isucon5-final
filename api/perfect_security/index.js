var http2 = require('http2')
  , fs = require('fs')
  , crypto = require('crypto');

var LISTEN_PORT = process.env.PORT || 8443;

var TOKEN_CIPHER_SECRET = new Buffer("yeeeeeah! miteru-? yeeeeeah! miteru-! yeeeeeah! miteru-?", "utf8");

var CONNECTED_TOKENS = {};

var ATTACKED_LAST_UPDATE = Date.now()
  , ATTACKED_NEXT_UPDATE = parseInt(ATTACKED_LAST_UPDATE + Math.random() * 15 * 1000);

// curl --http2 -v -k -H 'x-perfect-security-token: hoge' https://localhost:8082/tokens
// curl --http2 -v -k -H 'x-perfect-security-token: hoge' -H 'if-modified-since: Mon, 26 Oct 2015 18:22:54 GMT' https://localhost:8082/attacked_list

var options = {
  key: fs.readFileSync('./server.key'),
  cert: fs.readFileSync('./server.crt')
};

var KEY_CHARS = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789/='.split('');
var KEY_CHARS_NUM = KEY_CHARS.length;
var KEY_LENGTH = 20;

function random_char() {
  return KEY_CHARS[parseInt(Math.random() * KEY_CHARS_NUM)];
}

function generate_random_key() {
  var ary = [];
  for (var i = 0; i < KEY_LENGTH; i++) {
    ary.push(random_char());
  }
  return ary.join('');
}

function create_onetime_token(token_string) {
  var cipher = crypto.createCipher('aes-128-ecb', TOKEN_CIPHER_SECRET);
  var crypted = cipher.update(token_string, 'utf8', 'hex');
  return crypted + cipher.final('hex');
}

function auth_provider_handler(token, request, response) {
  var queries = request.url.split("?");
  queries.shift();
  var req = null;
  for (var i = 0; i< queries.length; i++) {
    if (queries[i].indexOf('req=') == 0) {
      req = queries[i].substring(4);
      break;
    }
  }
  if (req === null) {
    setTimeout(function(){
      response.statusCode = 400;
      response.end();
      delete CONNECTED_TOKENS[token];
    }, 200);
  } else {
    var key = generate_random_key(token);
    var onetime_token = create_onetime_token(token + ' ' + key + ' ' + req + ' ' + (new Date().getTime()));
    var responseJson = JSON.stringify({req: req, key: key, onetime_token: onetime_token});
    setTimeout(function(){
      response.writeHead(200, {
        'Content-Length': Buffer.byteLength(responseJson),
        'Content-Type': 'application/json'
      });
      response.write(responseJson, function(){
        response.end();
        delete CONNECTED_TOKENS[token];
      });
    }, 50);
  }
}

var WEEK_DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
var MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

function pad2(num) {
  if (num < 10)
    return '0' + num;
  return num;
}

function rfc1123(date) {
  // RFC 1123 (HTTP-Date)
  // Tue, 15 Nov 1994 12:45:26 GMT
  return WEEK_DAYS[date.getUTCDay()] + ", " + pad2(date.getUTCDate()) + " " + MONTHS[date.getUTCMonth()]
       + " " + date.getUTCFullYear() + " " + pad2(date.getUTCHours()) + ":" + pad2(date.getUTCMinutes())
       + ":" + pad2(date.getUTCSeconds()) + " GMT";
}

function attacked_tokens_handler(token, request, response) {
  var last_updated = new Date(ATTACKED_LAST_UPDATE);
  var epoch = parseInt(ATTACKED_LAST_UPDATE / 1000);
  var ifModifiedSince = request.headers['if-modified-since'];
  if ( ifModifiedSince !== undefined && parseInt(new Date(ifModifiedSince).getTime() / 1000) === epoch ) {
    setTimeout(function(){
      response.writeHead(304, {
        'Last-Modified': rfc1123(last_updated)
      });
      response.end();
      delete CONNECTED_TOKENS[token];
    }, 50);
  } else {
    var data = {
      key1: crypto.createHash('sha1').update(token + '001' + epoch + 'toooooonyyyyyy').digest('hex'),
      key2: crypto.createHash('sha1').update(token + '002' + epoch + 'toooooonyyyyyy').digest('hex'),
      key3: crypto.createHash('sha1').update(token + '003' + epoch + 'toooooonyyyyyy').digest('hex'),
      updated_at: epoch
    };
    var responseJson = JSON.stringify(data);
    var responseDelay = 500;
    setTimeout(function(){
      response.writeHead(200, {
        'Content-Length': Buffer.byteLength(responseJson),
        'Content-Type': 'application/json',
        'Last-Modified': rfc1123(last_updated)
      });
      response.write(responseJson, function(){
        response.end();
        delete CONNECTED_TOKENS[token];
      });
    }, responseDelay);
  }
}

setInterval(function(){
  if (Date.now() >= ATTACKED_NEXT_UPDATE) {
    ATTACKED_LAST_UPDATE = ATTACKED_NEXT_UPDATE;
    ATTACKED_NEXT_UPDATE = parseInt(ATTACKED_LAST_UPDATE + Math.random() * 60 * 1000);
  }
}, 3100);

http2.createServer(options, function(request, response) {
  var ver = request.httpVersion;
  var token = request.headers['x-perfect-security-token'];
  if (token === null || token === undefined) {
    setTimeout(function(){
      response.writeHead(403);
      response.end();
    }, 1000);
  } else if (ver !== '2.0' && CONNECTED_TOKENS[token] === 1) {
    response.statusCode = 429; // Too many requests
    response.end('Too many connections for token:' + token);
  } else {
    CONNECTED_TOKENS[token] = 1;
    if (request.url.indexOf('/tokens') === 0) {
      auth_provider_handler(token, request, response);
    } else if (request.url.indexOf('/attacked_list') === 0) {
      attacked_tokens_handler(token, request, response);
    } else {
      setTimeout(function(){
        response.writeHead(404);
        response.end();
        delete CONNECTED_TOKENS[token];
      }, 1000);
    }
  }
}).listen(LISTEN_PORT);
