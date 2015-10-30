# セットアップ
プロジェクトルートで以下を実行すれば、 http://localhost:8080/ で起動する。

```
$ gradle wrapper
$ ./gradlew run
```

# API
以下の2つ。

## 1. `/surname?q=<query>`
苗字検索。クエリには読みも漢字も指定できる。前方一致で最大30件が返ってくる。

### リクエスト例
- `/surname?q=%E3%81%BC`
   `ぼ`で始まる苗字
- `/surname?q=%E3%81%BB%E3%82%99`
   おなじく`ぼ`で始まる苗字。合成文字だが内部で正規化されるので同じ結果が返る

### レスポンス例

```
{
  "result": [
    {
      "name": "盆子原",
      "yomi": "ボンコハラ"
    },
    {
      "name": "盆子原",
      "yomi": "ボンコバラ"
    },
    {
      "name": "盆野",
      "yomi": "ボンノ"
    }
  ],
  "query": "ぼん"
}
```

## 2. `givenname?q=<query>`
名前検索。クエリには読みも漢字も指定できる。前方一致で最大30件が返ってくる。

### リクエスト例
- `/givenname?q=%E3%81%BC`
   `ぼ`で始まる名前
- `/givenname?q=%E3%81%BB%E3%82%99`
   おなじく`ぼ`で始まる名前。合成文字だが内部で正規化されるので同じ結果が返る

### レスポンス例

```
{
  "result": [
    {
      "name": "盆子原",
      "yomi": "ボンコハラ"
    },
    {
      "name": "盆子原",
      "yomi": "ボンコバラ"
    },
    {
      "name": "盆野",
      "yomi": "ボンノ"
    }
  ],
  "query": "ぼん"
}
```

## Benchmark

```
$ ab -c4 -t10 'http://127.0.0.1:8081/surname?q=%E3%81%BC'
This is ApacheBench, Version 2.3 <$Revision: 1663405 $>
Copyright 1996 Adam Twiss, Zeus Technology Ltd, http://www.zeustech.net/
Licensed to The Apache Software Foundation, http://www.apache.org/

Benchmarking 127.0.0.1 (be patient)
Completed 5000 requests
Completed 10000 requests
Finished 11508 requests


Server Software:        Apache-Coyote/1.1
Server Hostname:        127.0.0.1
Server Port:            8081

Document Path:          /surname?q=%E3%81%BC
Document Length:        1196 bytes

Concurrency Level:      4
Time taken for tests:   10.000 seconds
Complete requests:      11508
Failed requests:        0
Total transferred:      15938580 bytes
HTML transferred:       13763568 bytes
Requests per second:    1150.76 [#/sec] (mean)
Time per request:       3.476 [ms] (mean)
Time per request:       0.869 [ms] (mean, across all concurrent requests)
Transfer rate:          1556.45 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    0   0.5      0       5
Processing:     2    3   9.7      2     512
Waiting:        0    3   8.4      2     512
Total:          2    3   9.7      3     513

Percentage of the requests served within a certain time (ms)
  50%      3
  66%      3
  75%      4
  80%      4
  90%      4
  95%      5
  98%      6
  99%      7
 100%    513 (longest request)
```
