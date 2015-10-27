package main

import (
	"crypto/md5"
	"encoding/binary"
	"flag"
	"fmt"
	"net/http"
	"runtime"
	"time"

	"../log"
)

const ContentType = "application/json; charset=utf-8"

var (
	Wait     time.Duration
	Interval time.Duration
	Secret   = "happyhalloween"
)

func logHandler(f func(w http.ResponseWriter, r *http.Request) (int, string, string)) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()

		status, body, lastModified := f(w, r)

		if body != "" {
			w.Header().Set("Last-Modified", lastModified)
			w.Header().Set("Content-Type", ContentType)
			w.WriteHeader(status)
			w.Write([]byte(body))
		} else {
			w.WriteHeader(status)
		}

		if log.V(1) {
			end := time.Now()
			elapsed := end.Sub(start)
			log.Printf("time:%s	remote:%s	uri:%s	status:%d	reqtime:%.02f	body:%s lastmodified:%s",
				end.String(), r.RemoteAddr, r.RequestURI, status, elapsed.Seconds(), body, lastModified)
		}
	}
}

func handlerIndex(w http.ResponseWriter, r *http.Request) (int, string, string) {
	// 予報は一定時間ごとに切り替わる
	date := time.Now().Truncate(Interval)
	lastModified := date.Format(time.RFC1123)

	// If-Modified-Since と一致する場合は即座に応答する
	modifiedSince := r.Header.Get("If-Modified-Since")
	if modifiedSince == lastModified {
		return http.StatusNotModified, "", lastModified
	}

	// 遅延を表現する
	time.Sleep(Wait)

	// 天気を決める
	yoho := getResult(lastModified)

	result := fmt.Sprintf(`{"yoho": "%s", "date": "%s"}`, yoho, lastModified)
	return http.StatusOK, result, lastModified
}

func getResult(date string) string {
	num := toInt(date)
	if log.V(2) {
		log.Print("seed is %d", num)
	}
	idx := num % len(Tenkis)
	return Tenkis[idx]
}

func toInt(date string) int {
	str := fmt.Sprintf("%s %s", date, Secret)
	sum := md5.Sum([]byte(str))
	num := binary.BigEndian.Uint32(sum[:4])
	return int(num)
}

func main() {
	runtime.GOMAXPROCS(runtime.NumCPU())

	var verbosity int
	var port int
	var interval int
	var wait int

	flag.IntVar(&verbosity, "v", 0, "logging verbosity")
	flag.IntVar(&port, "p", 8988, "port")
	flag.IntVar(&wait, "w", 500, "wait each api call, milli seconds")
	flag.IntVar(&interval, "i", 3000, "interval yoho, milli seconds")
	flag.Parse()

	log.SetLevel(verbosity)

	Wait = time.Duration(wait) * time.Millisecond
	Interval = time.Duration(interval) * time.Millisecond

	http.HandleFunc("/", logHandler(handlerIndex))

	err := http.ListenAndServe(fmt.Sprintf(":%d", port), nil)
	if err != nil {
		log.Print(err)
	}
}
