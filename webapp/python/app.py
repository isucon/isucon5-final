# -*- coding: utf-8 -*-

import os
import subprocess
import json
import random
import urllib.parse
import urllib.request
import ssl
import bottle
import pg8000


app = bottle.default_app()
app.config.load_dict({
    "db": {
        "host": os.environ.get("ISUCON5_DB_HOST") or "localhost",
        "port": int(os.environ.get("ISUCON5_DB_PORT") or 5432),
        "username": os.environ.get("ISUCON5_DB_USER") or "isucon",
        "password": os.environ.get("ISUCON5_DB_PASSWORD") or None,
        "database": os.environ.get("ISUCON5_DB_NAME") or "isucon5f",
    },
    "session_secret": os.environ.get("ISUCON5_SESSION_SECRET") or "tonymoris",
})


def get_session_user_id():
    try:
        return bottle.request.get_cookie("user_id", secret=app.config["session_secret"])
    except ValueError:
        set_session_user_id(None)
        return None


def set_session_user_id(user_id):
    bottle.response.set_cookie("user_id", user_id, secret=app.config["session_secret"])


def db():
    try:
        return bottle.local.db
    except AttributeError:
        bottle.local.db = pg8000.connect(
            host=app.config["db.host"],
            port=app.config["db.port"],
            user=app.config["db.username"],
            password=app.config["db.password"],
            database=app.config["db.database"])
        return bottle.local.db


def db_fetchone(query, *args):
    args = args if args else None
    cursor = db().cursor()
    try:
        cursor.execute(query, args)
        return cursor.fetchone()
    finally:
        cursor.close()


def db_fetchall(query, *args):
    args = args if args else None
    cursor = db().cursor()
    try:
        cursor.execute(query, args)
        return cursor.fetchall()
    finally:
        cursor.close()


def db_execute(query, *args):
    args = args if args else None
    cursor = db().cursor()
    try:
        cursor.execute(query, args)
    except:
        db().rollback()
        raise
    else:
        db().commit()
    finally:
        cursor.close()


def authenticate(email, password):
    query = "SELECT id, email, grade FROM users WHERE email=%s AND passhash=digest(salt || %s, 'sha512')"
    rows = db_fetchall(query, email, password)
    if not rows:
        return None
    user = {"id": rows[0][0], "email": rows[0][1], "grade": rows[0][2]}
    set_session_user_id(user["id"])
    return user


def current_user():
    try:
        return bottle.request.user
    except AttributeError:
        user_id = get_session_user_id()
        if user_id:
            query = "SELECT id,email,grade FROM users WHERE id=%s"
            rows = db_fetchall(query, user_id)
            if rows:
                bottle.request.user = {"id": rows[0][0], "email": rows[0][1], "grade": rows[0][2]}
                return bottle.request.user
        set_session_user_id(None)
        bottle.request.user = None
        return bottle.request.user


def generate_salt():
    salts = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return "".join([salts[random.randint(0, len(salts)-1)] for _ in range(32)])


def fetch_api(method, uri, headers, params):
    assert method in ("GET", "POST")
    if params:
        query = urllib.parse.urlencode(params)
        uri += "?" + query
    context = None
    if uri.startswith("https://"):
        context = ssl.create_default_context()
        context.check_hostname = False
        context.verify_mode = ssl.CERT_NONE
    req = urllib.request.Request(uri, method=method, headers=headers)
    res = urllib.request.urlopen(req, context=context)
    body = res.read()
    return json.loads(body.decode("utf-8")) if body else {}


@app.get("/signup")
def get_signup():
    set_session_user_id(None)
    return bottle.template("signup")


@app.post("/signup")
def post_signup():
    email = bottle.request.forms.getunicode("email")
    password = bottle.request.forms.getunicode("password")
    grade = bottle.request.forms.getunicode("grade")
    salt = generate_salt()
    insert_user_query = "INSERT INTO users (email,salt,passhash,grade) VALUES (%s,%s,digest(%s || %s, 'sha512'),%s) RETURNING id"
    insert_subscription_query = "INSERT INTO subscriptions (user_id,arg) VALUES (%s,%s)"
    cursor = db().cursor()
    try:
        cursor.execute(insert_user_query, (email, salt, salt, password, grade, ))
        rows = cursor.fetchall()
        user_id = rows[0][0]
        cursor.execute(insert_subscription_query, (user_id, json.dumps({}), ))
    except:
        db().rollback()
        raise
    else:
        db().commit()
    bottle.redirect("/login")


@app.post("/cancel")
def post_cancel():
    bottle.redirect("/signup")


@app.get("/login")
def get_login():
    set_session_user_id(None)
    return bottle.template("login")


@app.post("/login")
def post_login():
    email = bottle.request.forms.getunicode("email")
    password = bottle.request.forms.getunicode("password")
    user = authenticate(email, password)
    if not user:
        bottle.abort(403)
    bottle.redirect("/")


@app.get("/logout")
def get_logout():
    set_session_user_id(None)
    bottle.redirect("/login")


@app.get("/")
def get_index():
    user = current_user()
    if not user:
        bottle.redirect("/login")
    return bottle.template("main", {"user": user})


@app.get("/user.js")
def get_userjs():
    user = current_user()
    if not user:
        bottle.abort(403)
    if user["grade"] == "micro":
        grade = 30000
    elif user["grade"] == "small":
        grade = 30000
    elif user["grade"] == "standard":
        grade = 20000
    elif user["grade"] == "premium":
        grade = 10000
    else:
        grade = ""
    bottle.response.set_header("Content-Type", "application/javascript")
    return bottle.template("userjs", {"grade": grade})


@app.get("/modify")
def get_modify():
    user = current_user()
    if not user:
        bottle.abort(403)
    query = "SELECT arg FROM subscriptions WHERE user_id=%s"
    rows = db_fetchall(query, user["id"])
    arg = rows[0][0]
    return bottle.template("modify", {"user": user, "arg": arg})


@app.post("/modify")
def post_modify():
    user = current_user()
    if not user:
        bottle.abort(403)
    
    service = bottle.request.forms.getunicode("service")
    
    token = bottle.request.forms.getunicode("token")
    token = token.strip() if token else None
    
    keys = bottle.request.forms.getunicode("keys")
    keys = keys.strip().split() if keys else None
    
    param_name = bottle.request.forms.getunicode("param_name")
    param_name = param_name.strip() if param_name else None
    
    param_value = bottle.request.forms.getunicode("param_value")
    param_value = param_value.strip() if param_value else None
    
    select_query = "SELECT arg FROM subscriptions WHERE user_id=%s FOR UPDATE"
    update_query = "UPDATE subscriptions SET arg=%s WHERE user_id=%s"
    cursor = db().cursor()
    try:
        cursor.execute(select_query, (user["id"], ))
        arg_json = cursor.fetchall()[0][0]
        arg = json.loads(arg_json) or {}
        arg.setdefault(service, {})
        if token:
            arg[service]["token"] = token
        if keys:
            arg[service]["keys"] = keys
        if param_name and param_value:
            arg[service].setdefault("params", {})
            arg[service]["params"][param_name] = param_value
        cursor.execute(update_query, (json.dumps(arg), user["id"], ))
    except:
        db().rollback()
        raise
    else:
        db().commit()
    finally:
        cursor.close()
    
    bottle.redirect("/modify")


@app.get("/data")
def get_data():
    user = current_user()
    if not user:
        bottle.abort(403)
    
    query = "SELECT arg FROM subscriptions WHERE user_id=%s"
    rows = db_fetchall(query, user["id"])
    arg_json = rows[0][0]
    arg = json.loads(arg_json)
    
    data = []
    for service, conf in arg.items():
        rows = db_fetchall("SELECT meth, token_type, token_key, uri FROM endpoints WHERE service=$1", service)
        method, token_type, token_key, uri_template = rows[0]
        headers = {}
        params = conf.get("params", {})
        if token_type == "header":
            headers[token_key] = conf["token"]
        elif token_type == "param":
            params[token_key] = conf["token"]
        conf_keys = conf.get("keys")
        if conf_keys:
            uri = uri_template % conf_keys
        else:
            uri = uri_template
        data.append({"service": service, "data": fetch_api(method, uri, headers, params)})
    
    bottle.response.content_type = "application/json; charset=utf-8"
    return json.dumps(data)


@app.get("/css/<filename:path>")
def get_css(filename):
    return get_static("css", filename)


@app.get("/fonts/<filename:path>")
def get_fonts(filename):
    return get_static("fonts", filename)


@app.get("/js/<filename:path>")
def get_js(filename):
    return get_static("js", filename)


def get_static(dirname, filename):
    basedir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    staticdir = os.path.join(basedir, "static", dirname)
    return bottle.static_file(filename, root=staticdir)


@app.get("/initialize")
def get_initialize():
    basedir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    sqlfile = os.path.join(basedir, "sql", "initialize.sql")
    subprocess.call("psql -f " + sqlfile + " isucon5f", shell=True)
    return ""


bottle.BaseTemplate.defaults = {
    "db": db,
}

if __name__ == "__main__":
    app.run(server="wsgiref",
            host="127.0.0.1",
            port=8080,
            reloader=False,
            quiet=False,
            debug=True)
