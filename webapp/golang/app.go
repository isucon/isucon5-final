package main

import (
	"crypto/tls"
	"database/sql"
	"encoding/json"
	"fmt"
	"github.com/gorilla/context"
	"github.com/gorilla/mux"
	"github.com/gorilla/sessions"
	_ "github.com/lib/pq"
	"html/template"
	"log"
	"math/rand"
	"net/http"
	"net/url"
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
)

var (
	db    *sql.DB
	store *sessions.CookieStore
)

type User struct {
	ID    int
	Email string
	Grade string
}

type Arg map[string]*Service

type Service struct {
	Token  string            `json:"token"`
	Keys   []string          `json:"keys"`
	Params map[string]string `json:"params"`
}

type Data struct {
	Service string                 `json:"service"`
	Data    map[string]interface{} `json:"data"`
}

var saltChars = []rune("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")

func getSession(w http.ResponseWriter, r *http.Request) *sessions.Session {
	session, _ := store.Get(r, "isucon5q-go.session")
	return session
}

func getTemplatePath(file string) string {
	return path.Join("templates", file)
}

func render(w http.ResponseWriter, r *http.Request, status int, file string, data interface{}) {
	tpl := template.Must(template.New(file).ParseFiles(getTemplatePath(file)))
	w.WriteHeader(status)
	checkErr(tpl.Execute(w, data))
}

func authenticate(w http.ResponseWriter, r *http.Request, email, passwd string) *User {
	query := `SELECT id, email, grade FROM users WHERE email=$1 AND passhash=digest(salt || $2, 'sha512')`
	row := db.QueryRow(query, email, passwd)
	user := User{}
	err := row.Scan(&user.ID, &user.Email, &user.Grade)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil
		}
		checkErr(err)
	}
	session := getSession(w, r)
	session.Values["user_id"] = user.ID
	session.Save(r, w)
	return &user
}

func getCurrentUser(w http.ResponseWriter, r *http.Request) *User {
	u := context.Get(r, "user")
	if u != nil {
		user := u.(User)
		return &user
	}
	session := getSession(w, r)
	userID, ok := session.Values["user_id"]
	if !ok || userID == nil {
		return nil
	}
	row := db.QueryRow(`SELECT id,email,grade FROM users WHERE id=$1`, userID)
	user := User{}
	err := row.Scan(&user.ID, &user.Email, &user.Grade)
	if err == sql.ErrNoRows {
		clearSession(w, r)
		return nil
	}
	checkErr(err)
	context.Set(r, "user", user)
	return &user
}

func generateSalt() string {
	salt := make([]rune, 32)
	for i := range salt {
		salt[i] = saltChars[rand.Intn(len(saltChars))]
	}
	return string(salt)
}

func clearSession(w http.ResponseWriter, r *http.Request) {
	session := getSession(w, r)
	delete(session.Values, "user_id")
	session.Save(r, w)
}

func GetSignUp(w http.ResponseWriter, r *http.Request) {
	clearSession(w, r)
	render(w, r, http.StatusOK, "signup.html", nil)
}

func PostSignUp(w http.ResponseWriter, r *http.Request) {
	email := r.FormValue("email")
	passwd := r.FormValue("password")
	grade := r.FormValue("grade")
	salt := generateSalt()
	insertUserQuery := `INSERT INTO users (email,salt,passhash,grade) VALUES ($1,$2,digest($3 || $4, 'sha512'),$5) RETURNING id`
	insertSubscriptionQuery := `INSERT INTO subscriptions (user_id,arg) VALUES ($1,$2)`
	tx, err := db.Begin()
	checkErr(err)
	row := tx.QueryRow(insertUserQuery, email, salt, salt, passwd, grade)

	var userId int
	err = row.Scan(&userId)
	if err != nil {
		tx.Rollback()
		checkErr(err)
	}
	_, err = tx.Exec(insertSubscriptionQuery, userId, "{}")
	if err != nil {
		tx.Rollback()
		checkErr(err)
	}
	checkErr(tx.Commit())
	http.Redirect(w, r, "/login", http.StatusSeeOther)
}

func PostCancel(w http.ResponseWriter, r *http.Request) {
	http.Redirect(w, r, "/signup", http.StatusSeeOther)
}

func GetLogin(w http.ResponseWriter, r *http.Request) {
	clearSession(w, r)
	render(w, r, http.StatusOK, "login.html", nil)
}

func PostLogin(w http.ResponseWriter, r *http.Request) {
	email := r.FormValue("email")
	passwd := r.FormValue("password")
	authenticate(w, r, email, passwd)
	if getCurrentUser(w, r) == nil {
		http.Error(w, "Failed to login.", http.StatusForbidden)
		return
	}
	http.Redirect(w, r, "/", http.StatusSeeOther)
}

func GetLogout(w http.ResponseWriter, r *http.Request) {
	clearSession(w, r)
	http.Redirect(w, r, "/login", http.StatusSeeOther)
}

func GetIndex(w http.ResponseWriter, r *http.Request) {
	if getCurrentUser(w, r) == nil {
		http.Redirect(w, r, "/login", http.StatusSeeOther)
		return
	}
	render(w, r, http.StatusOK, "main.html", struct{ User User }{*getCurrentUser(w, r)})
}

func GetUserJs(w http.ResponseWriter, r *http.Request) {
	if getCurrentUser(w, r) == nil {
		http.Error(w, "Failed to login.", http.StatusForbidden)
		return
	}
	render(w, r, http.StatusOK, "user.js", struct{ Grade string }{getCurrentUser(w, r).Grade})
}

func GetModify(w http.ResponseWriter, r *http.Request) {
	user := getCurrentUser(w, r)
	if user == nil {
		http.Error(w, "Failed to login.", http.StatusForbidden)
		return
	}
	row := db.QueryRow(`SELECT arg FROM subscriptions WHERE user_id=$1`, user.ID)
	var arg string
	err := row.Scan(&arg)
	if err == sql.ErrNoRows {
		arg = "{}"
	}
	render(w, r, http.StatusOK, "modify.html", struct {
		User User
		Arg  string
	}{*user, arg})
}

func PostModify(w http.ResponseWriter, r *http.Request) {
	user := getCurrentUser(w, r)
	if user == nil {
		http.Error(w, "Failed to login.", http.StatusForbidden)
		return
	}

	service := r.FormValue("service")
	token := r.FormValue("token")
	keysStr := r.FormValue("keys")
	keys := []string{}
	if keysStr != "" {
		keys = regexp.MustCompile("\\s+").Split(keysStr, -1)
	}
	paramName := r.FormValue("param_name")
	paramValue := r.FormValue("param_value")

	selectQuery := `SELECT arg FROM subscriptions WHERE user_id=$1 FOR UPDATE`
	updateQuery := `UPDATE subscriptions SET arg=$1 WHERE user_id=$2`

	tx, err := db.Begin()
	checkErr(err)
	row := tx.QueryRow(selectQuery, user.ID)
	var jsonStr string
	err = row.Scan(&jsonStr)
	if err == sql.ErrNoRows {
		jsonStr = "{}"
	} else if err != nil {
		tx.Rollback()
		checkErr(err)
	}
	var arg Arg
	err = json.Unmarshal([]byte(jsonStr), &arg)
	if err != nil {
		tx.Rollback()
		checkErr(err)
	}

	if _, ok := arg[service]; !ok {
		arg[service] = &Service{}
	}
	if token != "" {
		arg[service].Token = token
	}
	if len(keys) > 0 {
		arg[service].Keys = keys
	}
	if arg[service].Params == nil {
		arg[service].Params = make(map[string]string)
	}
	if paramName != "" && paramValue != "" {
		arg[service].Params[paramName] = paramValue
	}

	b, err := json.Marshal(arg)
	if err != nil {
		tx.Rollback()
		checkErr(err)
	}
	_, err = tx.Exec(updateQuery, string(b), user.ID)
	checkErr(err)

	tx.Commit()

	http.Redirect(w, r, "/modify", http.StatusSeeOther)
}

func fetchApi(method, uri string, headers, params map[string]string) map[string]interface{} {
	client := &http.Client{}
	if strings.HasPrefix(uri, "https://") {
		tr := &http.Transport{
			TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
		}
		client.Transport = tr
	}
	values := url.Values{}
	for k, v := range params {
		values.Add(k, v)
	}

	var req *http.Request
	var err error
	switch method {
	case "GET":
		req, err = http.NewRequest(method, uri, nil)
		checkErr(err)
		req.URL.RawQuery = values.Encode()
		break
	case "POST":
		req, err = http.NewRequest(method, uri, strings.NewReader(values.Encode()))
		checkErr(err)
		break
	}

	for k, v := range headers {
		req.Header.Add(k, v)
	}
	resp, err := client.Do(req)
	checkErr(err)

	defer resp.Body.Close()

	var data map[string]interface{}
	d := json.NewDecoder(resp.Body)
	d.UseNumber()
	checkErr(d.Decode(&data))
	return data
}

func GetData(w http.ResponseWriter, r *http.Request) {
	user := getCurrentUser(w, r)
	if user == nil {
		w.WriteHeader(http.StatusForbidden)
		return
	}

	row := db.QueryRow(`SELECT arg FROM subscriptions WHERE user_id=$1`, user.ID)
	var argJson string
	checkErr(row.Scan(&argJson))
	var arg Arg
	checkErr(json.Unmarshal([]byte(argJson), &arg))

	data := make([]Data, 0, len(arg))
	for service, conf := range arg {
		row := db.QueryRow(`SELECT meth, token_type, token_key, uri FROM endpoints WHERE service=$1`, service)
		var method string
		var tokenType *string
		var tokenKey *string
		var uriTemplate *string
		checkErr(row.Scan(&method, &tokenType, &tokenKey, &uriTemplate))

		headers := make(map[string]string)
		params := conf.Params
		if params == nil {
			params = make(map[string]string)
		}

		if tokenType != nil && tokenKey != nil {
			switch *tokenType {
			case "header":
				headers[*tokenKey] = conf.Token
				break
			case "param":
				params[*tokenKey] = conf.Token
				break
			}
		}

		ks := make([]interface{}, len(conf.Keys))
		for i, s := range conf.Keys {
			ks[i] = s
		}
		uri := fmt.Sprintf(*uriTemplate, ks...)

		data = append(data, Data{service, fetchApi(method, uri, headers, params)})
	}

	w.Header().Set("Content-Type", "application/json")
	body, err := json.Marshal(data)
	checkErr(err)
	w.Write(body)
}

func GetInitialize(w http.ResponseWriter, r *http.Request) {
	fname := "../sql/initialize.sql"
	file, err := filepath.Abs(fname)
	checkErr(err)
	_, err = exec.Command("psql", "-f", file, "isucon5f").Output()
	checkErr(err)
}

func main() {
	host := os.Getenv("ISUCON5_DB_HOST")
	if host == "" {
		host = "localhost"
	}
	portstr := os.Getenv("ISUCON5_DB_PORT")
	if portstr == "" {
		portstr = "5432"
	}
	port, err := strconv.Atoi(portstr)
	if err != nil {
		log.Fatalf("Failed to read DB port number from an environment variable ISUCON5_DB_PORT.\nError: %s", err.Error())
	}
	user := os.Getenv("ISUCON5_DB_USER")
	if user == "" {
		user = "isucon"
	}
	password := os.Getenv("ISUCON5_DB_PASSWORD")
	dbname := os.Getenv("ISUCON5_DB_NAME")
	if dbname == "" {
		dbname = "isucon5f"
	}
	ssecret := os.Getenv("ISUCON5_SESSION_SECRET")
	if ssecret == "" {
		ssecret = "tonymoris"
	}

	db, err = sql.Open("postgres", "host="+host+" port="+strconv.Itoa(port)+" user="+user+" dbname="+dbname+" sslmode=disable password="+password)
	if err != nil {
		log.Fatalf("Failed to connect to DB: %s.", err.Error())
	}
	defer db.Close()

	store = sessions.NewCookieStore([]byte(ssecret))

	r := mux.NewRouter()

	s := r.Path("/signup").Subrouter()
	s.Methods("GET").HandlerFunc(GetSignUp)
	s.Methods("POST").HandlerFunc(PostSignUp)

	l := r.Path("/login").Subrouter()
	l.Methods("GET").HandlerFunc(GetLogin)
	l.Methods("POST").HandlerFunc(PostLogin)

	r.HandleFunc("/logout", GetLogout).Methods("GET")

	m := r.Path("/modify").Subrouter()
	m.Methods("GET").HandlerFunc(GetModify)
	m.Methods("POST").HandlerFunc(PostModify)

	r.HandleFunc("/data", GetData).Methods("GET")

	r.HandleFunc("/cancel", PostCancel).Methods("POST")

	r.HandleFunc("/user.js", GetUserJs).Methods("GET")

	r.HandleFunc("/initialize", GetInitialize).Methods("GET")

	r.HandleFunc("/", GetIndex)
	r.PathPrefix("/").Handler(http.FileServer(http.Dir("../static")))
	log.Fatal(http.ListenAndServe(":8080", r))
}

func checkErr(err error) {
	if err != nil {
		panic(err)
	}
}
