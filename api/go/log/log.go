package log

import (
	"io"
	"log"
	"os"
	"sync"
	"sync/atomic"
)

var (
	mutex     sync.RWMutex
	logger    *log.Logger = nil
	verbosity int32       = 0
)

func init() {
	setLogger(os.Stdout)
}

func setLogger(out io.Writer) {
	mutex.Lock()
	logger = log.New(out, "", 0)
	mutex.Unlock()
}

func getLogger() *log.Logger {
	mutex.RLock()
	lgr := logger
	mutex.RUnlock()
	return lgr
}

func SetOutput(out io.Writer) {
	setLogger(out)
}

func SetLevel(level int) {
	atomic.StoreInt32(&verbosity, int32(level))
}

func V(level int) bool {
	return int32(level) <= atomic.LoadInt32(&verbosity)
}

func Print(v ...interface{}) {
	lgr := getLogger()
	if lgr != nil {
		lgr.Print(v)
	}
}

func Println(v ...interface{}) {
	lgr := getLogger()
	if lgr != nil {
		lgr.Println(v)
	}
}

func Printf(format string, v ...interface{}) {
	lgr := getLogger()
	if lgr != nil {
		lgr.Printf(format, v...)
	}
}
