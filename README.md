# ISUCON5 決勝

## ライセンス

The MIT License (MIT)

Copyright (c) 2015 tagomoris, kamipo, najeira, hkurokawa, making, xerial, hokaccha

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

# Webアプリケーション仕様

Cookieでセッション管理、ログインを要求する。

なおサインアップ時に指定する grade で使える外部APIのセットが決まり `/modify` で各APIの引数を与えることとする。

## 外部APIセット(未決定)

各gradeが使えるAPIは以下の通り

* premium: 全て
* standard: xxx以外全て
* small: xxx, xxx, ....
* micro: ken, ken2, ...

## 外部API作成仕様

外部APIは以下のようなものである必要があります。

* GETもしくはPOSTリクエストを受ける
* JSONのオブジェクトを返す
* URIのpath部分は固定文字列、もしくは任意個数文字列の埋め込みで成る (ex: `/part1/%s/part2/%s`)
* フォームもしくはURIクエリパラメータでキーと値のペアをひとつだけ取れる (ex: `?q=VALUE`)
* tokenをとれる
  * とらなくてもよい
  * トークンはフォームもしくはURIクエリパラメータのひとつ(`param`)、もしくはHTTPヘッダ(`header`)として渡す
  * トークンはユーザごとに固定のものとする
* レスポンスJSONはできるだけリクエストがどのようなものだったかが分かるような情報を含んで返すこと
  * これはアプリケーション側でもレンダリングし、ベンチマークツールが結果のチェックに使用する

これらのリクエスト組み立て方法はWebアプリケーション側の `endpoints` テーブルに入るレコードによって制御します。このテーブルは全ユーザに同じデータが渡されます。

アプリケーション参考実装は外部APIの情報を以下の場所に持っています。つまり、外部APIをひとつ足すためには `api/` 以下に足すアプリケーションコードのほか、各言語の参考実装に以下の変更をする必要があります。(が、そう多くはありません。)

* `endpoints` へのレコードの追加 (`sql/init.sql` への行の追加)
* `static/airisu.js` へのレンダリング用関数の追加 (`render_xxx()` 関数の追加、および `render()` の switch-case への行の追加)
* 各言語実装における API 登録画面へのHTML1行の追加 (rubyの場合は `views/modify.erb` への `<div class="api-form">` のタグ部分)
  * これは実際にはJSでレンダリングされるため各実装言語のロジックは必要ない

以上です。が、面倒であれば適当に後回しにしてください。(ただし何を後回しにしたかどこかにメモっておいてもらえると助かります)

## リクエストハンドラ

* `GET /signup` サインアップ用フォーム表示
* `POST /signup` サインアップ、成功したら `/login` にリダイレクト
* `POST /cancel` 解約、そのユーザのデータをすべて削除する、完了したら `/signup` にリダイレクト

* `GET /login` ログインフォームを含むHTMLを返す
* `POST /login` ログインに成功したら `/`、失敗したら `/login` にリダイレクト
  * ログインしていなければ 403

* `GET /` HTMLを返す、この段階では外部APIへのリクエストは発生しない (未ログインの場合 `/signup` にリダイレクト)
* `GET /user.js` ユーザごとにAPIリクエスト用のjsを返す
  * ユーザ情報のgradeを見て異なる auto refresh の間隔が入ったjavascriptを返す
  * 高速化のためにはgradeごとにjsを予め生成しておいてそこにredirectすればよいようにする
  * ログインしていなければ 403

* `GET /modify` APIアクセス情報変更画面を表示
* `POST /modify` APIアクセス情報の変更を行う
  * ログインしていなければ 403
  * なお各APIアクセス情報は(必要なら)以下のキーを持つ
  * `keys` リスト、pathの `%s` にsprintfで展開される
  * `params` オブジェクト、GETならクエリパラメータ、POSTならフォームデータとして送信される
  * `token` 文字列、存在すれば `token_type` および `token_key` にしたがって送信される
  * 成功したら `/modify` にリダイレクト

* `GET /data` ユーザがsubscribeしているAPIすべてにアクセスし、結果をまとめてjsonで返す
  * ログインしてなければ 403

* `GET /initialize` データの初期化用ハンドラ
