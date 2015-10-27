package main

import (
	"crypto/md5"
	"encoding/binary"
	"flag"
	"fmt"
)

func toInt(date, secret string) int {
	str := fmt.Sprintf("%s %s", date, secret)
	sum := md5.Sum([]byte(str))
	num := binary.BigEndian.Uint32(sum[:4])
	return int(num)
}

func main() {
	var date string
	var secret string
	flag.StringVar(&date, "d", "", "date string")
	flag.StringVar(&secret, "s", "happyhalloween", "secret")
	flag.Parse()

	// Tue, 27 Oct 2015 08:46:40 JST ==> 51498849
	// Tue, 27 Oct 2015 08:46:41 JST ==> 1366538060
	// Tue, 27 Oct 2015 08:46:42 JST ==> 1443194715
	n := toInt(date, secret)
	fmt.Println(n)
}
