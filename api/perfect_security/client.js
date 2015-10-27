var opt = {
  host: 'localhost',
  port: 8082,
  path: '/tokens',
  headers: { 'x-perfect-security-token': 'xxxxxxxxxxx' },
  cert: require('fs').readFileSync('./server.crt'),
  rejectUnauthorized: false
};
var req = require('http2').request(opt, function(res) {
  console.log('code:' + res.statusCode);
  res.on('data', function(chunk){ console.log(chunk.toString()); });
  res.on('end', function(){
    console.log('end.');
  });
});

