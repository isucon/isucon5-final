# ISUCON5 決勝イメージの公開とその使用方法

tagomorisです。もうISUCON5決勝から1ヶ月が経ってしまいましたが、いくつか準備も整えまして、ISUCON5決勝で用いた環境のGoogle Cloud Platform用イメージを用意できましたので公開します。

* HTTPS: https://storage.googleapis.com/isucon5-images/isucon5-final-1.tar.gz
* GCS: gs://isucon5-images/isucon5-final-1.tar.gz

ISUCON5決勝では参加者用Webアプリケーションおよびベンチマークツールの他に外部APIのサーバプロセスも必要でしたが、このイメージはそれらを全て含んでいます。インスタンスを作成して起動すればベンチマークが通る状態で起動するはずです。

## イメージの取り込み

Google Cloud Platformでは上記URLからイメージとして取り込む作業を行う必要があります。[GCP Developers Console](https://console.developers.google.com)にログイン後、[Compute EngineのImages](https://console.developers.google.com/compute/images)から `[+] NEW IMAGE` を開きます。

![Create a new image](https://i.gyazo.com/8160d98ce2f2513fb2617eb7c401aa97.png)

Name は適当で構いません。Source は "Cloud Storage file" を選択し、その下の入力欄に `isucon5-images/isucon5-final-1.tar.gz` を入力します。正常に認識されたら `Create` のボタンが有効になるはずなので、クリックすれば完了です。

CLIからだと `gcloud compute images create IMAGE_NAME --source-uri URI` で行えます。URIには上述 https のURIを指定します。

## インスタンス作成、ベンチマーク実行

あとはインスタンス作成時にイメージとして取り込んだ時の名前を指定して実行するだけです。インスタンスタイプなどは適当なものを使用すればよいと思います(決勝同等の環境の作成については後述)が、少なくともCPU数が4以上のものを選択すると良いと思います。Webアプリケーション・ベンチマークツールに加えてAPIサーバが複数プロセス起動するためです。1〜2CPUでは普通のスコアは出ないのではないかと思います。
また作成時には "Allow HTTP traffic" を有効にしておくと良いでしょう。

起動したら `gcloud compute ssh INSTANCE_NAME` でログイン後、以下のコマンドを実行すればベンチマークが実行されます。

```
sudo su - isucon
cd /home/isucon/isucon5-final/bench/
export ISUCON_BENCH_DATADIR=$(pwd)/json
cat ../data/source.json | ruby -rjson -e 'puts JSON.parse(STDIN.read)[0].to_json' | gradle run -Pargs="net.isucon.isucon5f.bench.Full 127.0.0.1"
```

最後のコマンドが実際のベンチマーク実行です。`[0]` から `[19]` までの20セットが使えます。ベンチマークが走りきれば、以下のような結果が出力されるでしょう。

```
{
  "valid" : true,
  "requests" : 1079,
  "elapsed" : 77266,
  "done" : "[{Init},{Bootstrap},{Checker,ModifyLoader,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load,Load}]",
  "responses" : {
    "success" : 997,
    "redirect" : 82,
    "failure" : 0,
    "error" : 0,
    "exception" : 0
  },
  "violations" : [ ]
}
```

このコマンドでは直接のスコア算出までは行われませんが、簡単に算出できます。詳しくは以下のレギュレーション文書をご確認ください。

[ISUCON5 決勝レギュレーション ルール詳細](https://gist.github.com/tagomoris/2be8751d8c13f5e78e61#ルール詳細)

これで作業をする準備は完了です。

## イメージでの変更点

このイメージはAll-in-oneイメージとするため、以下の点についてイベント当日から修正しています。

* API Endpointの接続先を `/etc/hosts` の修正により `127.0.0.1` に変更
* KEN API EndpointのListen Portを `/home/isucon/webapp/sql/initialize.sql` の修正により `8082` に変更
* KEN API EndpointのServer Process数を `64` から `8` に変更

また[先日のエントリ](http://isucon.net/archives/45905117.html)にて言及していたチェッカーのバグについては修正済みになっています。

## 決勝同等の環境の作成

All-in-oneイメージはISUCON5決勝の問題を体験するには便利ですが、スコアは実際の決勝イベントとは較べられないものになります。これはベンチマークツールやAPIサーバが同一ホスト上で動いていることなどによります。

決勝と同じような環境を作成したい場合、以下のような点に気をつければよいでしょう。

* API, Bench, WebApp(x3) 用の5インスタンスを作成(全て同一イメージでよい)
* APIサーバ： n1-highcpu-16 or n1-highcpu-32 (決勝イベントでは12core, 24vCPU)
* APIサーバ作成時、ネットワーク設定として HTTP, HTTPS のほかに port 8081, 8082, 8988 へのアクセスを有効にする
* Benchサーバ： n1-highcpu-8 or n1-highcpu-16 (決勝イベントでは6core, 12vCPU)
* WebAppサーバ: n1-highcpu-4 (決勝イベントでは 3core, メモリ4GB)
* WebAppサーバの `/etc/hosts` において `api.five-final.isucon.net` の行の宛先IPアドレスにAPIサーバのPublic IP Addressを記述する

上記構成でインスタンスを起動後、BenchサーバからWebAppサーバ(のどれか1台のIPアドレス)を指定してベンチマークを実行すれば、ほぼ決勝と同じ環境になっていると言えると思います。またごく最近、[Google Compute EngineではCPU/メモリをカスタマイズしたインスタンスを作成](http://googlecloudplatform.blogspot.jp/2015/11/introducing-Custom-Machine-Types-the-freedom-to-configure-the-best-VM-shape-for-your-workload.html)できるようになりました。これを使って決勝の構成とぴったり同じCPU数・メモリ容量にしてみるのも良いかもしれません。
