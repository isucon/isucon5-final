# KEN_ALL API

```sh
bundle exec unicorn -c unicorn_config.rb
```

**Performance: 820.99 [#/sec]**

```
$ ab -c4 -t10 http://127.0.0.1:8080/9220011
This is ApacheBench, Version 2.3 <$Revision: 1663405 $>
Copyright 1996 Adam Twiss, Zeus Technology Ltd, http://www.zeustech.net/
Licensed to The Apache Software Foundation, http://www.apache.org/

Benchmarking 127.0.0.1 (be patient)
Completed 5000 requests
Completed 10000 requests
Finished 13870 requests


Server Software:        
Server Hostname:        127.0.0.1
Server Port:            8080

Document Path:          /9220011
Document Length:        176 bytes

Concurrency Level:      4
Time taken for tests:   16.894 seconds
Complete requests:      13870
Failed requests:        0
Total transferred:      4896110 bytes
HTML transferred:       2441120 bytes
Requests per second:    820.99 [#/sec] (mean)
Time per request:       4.872 [ms] (mean)
Time per request:       1.218 [ms] (mean, across all concurrent requests)
Transfer rate:          283.02 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    1  45.8      0    2701
Processing:     1    2   1.0      2      66
Waiting:        1    2   0.8      1      56
Total:          1    3  45.9      2    2704
WARNING: The median and mean for the waiting time are not within a normal deviation
        These results are probably not that reliable.

Percentage of the requests served within a certain time (ms)
  50%      2
  66%      2
  75%      2
  80%      2
  90%      2
  95%      3
  98%      3
  99%      4
 100%   2704 (longest request)
```
