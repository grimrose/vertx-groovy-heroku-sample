Vert.x WebSocket Application Sample on Heroku
================================================

[Heroku]上で[Vert.x]のWebSocketアプリケーションを動かす為のサンプルです。


## 使い方

### ローカルで動かす場合

#### [Vert.x]

[Installation Guide]を参考にしてください。

[GVM]を使ってインストールすると簡単です。

`$ gvm install vertx`


#### [MongoDB]

[Install MongoDB]を参考にしてください。

設定ファイルとして`conf.json`を作成して、内容を以下の様にしてください。

```javascript
{
    "MONGOLAB_URI": "mongodb://<dbuser>:<dbpass>@<host>:<port>/<db_name>"
}
```


#### 起動

以下のコマンドで起動します。

`$ vertx run server.groovy -conf conf.json`

Webブラウザで[localhost:9000](localhost:9000)にアクセスして表示されれば成功です。


### Herokuで動かす場合

#### [Vert.x]

[tomaslin / heroku-buildpack-vertx-jdk7]を参考にしてください。

以下のコマンドで[Vert.x]用のbuildpackが登録されます。

`$ heroku create --stack cedar --buildpack https://github.com/tomaslin/heroku-buildpack-vertx-jdk7.git`


#### [MongoDB]

[Adding MongoLab]を参考に追加してください。


#### WebSocket

このアプリケーションはWebSocketを利用しているので、[Heroku Labs: WebSockets]を参考に、利用できるようにします。


#### デプロイ

以下のコマンドでデプロイします。

`$ git push heroku master`


## ライセンス

Copyright (c) 2014 grimrose
See the LICENSE file for license rights and limitations (MIT).

[Heroku]: https://www.heroku.com
[Vert.x]: http://vertx.io
[Installation Guide]: http://vertx.io/install.html
[GVM]: http://gvmtool.net
[MongoDB]: https://www.mongodb.org
[Install MongoDB]: http://docs.mongodb.org/manual/installation/#install-mongodb
[Adding MongoLab]: https://devcenter.heroku.com/articles/mongolab#adding-mongolab
[MongoLab]: https://addons.heroku.com/mongolab
[tomaslin / heroku-buildpack-vertx-jdk7]: https://github.com/tomaslin/heroku-buildpack-vertx-jdk7
[Heroku Labs: WebSockets]: https://devcenter.heroku.com/articles/heroku-labs-websockets
