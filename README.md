# Webアプリケーション仕様

Cookieでセッション管理、ログインを要求する。

なおサインアップ時に指定する grade で使える外部APIのセットが決まり `/modify` で各APIの引数を与えることとする。

## 外部APIセット(未決定)

各gradeが使えるAPIは以下の通り

* premium: 全て
* standard: xxx以外全て
* small: xxx, xxx, ....
* micro: ken, ken2, ...

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
