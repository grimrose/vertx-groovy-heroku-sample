import groovy.json.JsonSlurper
import org.vertx.groovy.core.buffer.Buffer
import org.vertx.groovy.core.eventbus.Message
import org.vertx.groovy.core.http.HttpServerRequest
import org.vertx.groovy.core.http.RouteMatcher
import org.vertx.groovy.core.http.ServerWebSocket
import org.vertx.java.core.json.impl.Json

String mongoAddress = 'app.mongo'

// vert.x objects
def logger = container.logger
def eb = vertx.eventBus
def appConf = container.config

// setup mongo
URI mongoUri = new URI(container.env['MONGOLAB_URI'] ?: appConf['MONGOLAB_URI'])
def mongoConf = [
        "address": mongoAddress,
        "host": mongoUri.host,
        "port": mongoUri.port,
        "username": mongoUri.userInfo.split(':')[0],
        "password": mongoUri.userInfo.split(':')[1],
        "db_name": mongoUri.path.replace('/', '')
]
logger.debug mongoConf

// deploy mod mongo
container.deployModule("io.vertx~mod-mongo-persistor~2.1.0", mongoConf) { asyncResult ->
    if (asyncResult.succeeded) {
        def cmd = [
                action: "command",
                command: "{ ping: 1 }"
        ]
        eb.send(mongoAddress, cmd) { Message message ->
            logger.info "mongo_status: ${message.body()}"
        }
    } else {
        logger.error "Failed to deploy"
        asyncResult.cause.printStackTrace()
    }
}

// save message
eb.registerHandler("messages.save") { Message message ->
    def cmd = [
            action: "save",
            collection: "messages",
            document: [
                    content: message.body(),
                    post_at: new Date()
            ]
    ]
    eb.send(mongoAddress, cmd) { Message reply ->
        message.reply(reply.body())
    }
}

// find message
eb.registerHandler("messages.find") { Message message ->
    def condition = message.body()
    def cmd = [
            action: "find",
            collection: "messages",
            matcher: condition
    ]
    eb.send(mongoAddress, cmd) { Message response ->
        message.reply(response.body())
    }
}

// delete message
eb.registerHandler("messages.delete") { Message message ->
    def condition = message.body()
    def cmd = [
            action: "delete",
            collection: "messages",
            matcher: condition
    ]
    eb.send(mongoAddress, cmd) { Message response ->
        message.reply(response.body())
    }
}

// watchdog mongo
long delay = 1000 * 60 * 60 //=> per hour
def limit = 100

vertx.setPeriodic(delay) { timerID ->
    def cmd = [
            "action": "collection_stats",
            "collection": "messages"
    ]
    eb.send(mongoAddress, cmd) { Message response ->
        def result = response.body()
        if (result.status == 'ok') {
            def count = result.stats.count
            logger.info "current_count: ${count}"
            if (limit < count) {
                def condition = [
                        "post_at": "{ \$lt: ${new Date().time - delay} }"
                ]
                eb.send("messages.delete", condition) { Message reply ->
                    logger.info reply.body()
                }
            }
        } else {
            logger.error result.message
        }
    }
}

def server = vertx.createHttpServer()

// route matcher
RouteMatcher matcher = new RouteMatcher()
matcher.get("/") { request ->
    request.response.sendFile('index.html')
}

matcher.noMatch { HttpServerRequest request ->
    request.response.setStatusCode(404)
    request.response.end()
}

server.requestHandler(matcher.asClosure())

// WebSocket
server.websocketHandler { ServerWebSocket ws ->
    if (ws.path == '/ws') {
        ws.dataHandler { Buffer data ->
            logger.info(data)
            def json = new JsonSlurper().parseText(data.toString())
            switch (json.command) {
                case ~/save/:
                    eb.send("messages.save", json.message) { Message reply ->
                        if (reply.body().status == "ok") {
                            def condition = [_id: reply.body._id]
                            eb.send("messages.find", condition) { Message response ->
                                def results = response.body().results
                                results.each { element ->
                                    ws.writeTextFrame(Json.encode([command: "findById", result: element]))
                                }
                            }
                        } else {
                            ws.writeTextFrame(Json.encode(reply.body()))
                        }
                    }
                    break
                case ~/delete/:
                    eb.send("messages.delete", [_id: json.message_id]) { Message reply ->
                        if (reply.body().status == "ok") {
                            ws.writeTextFrame(Json.encode([command: "delete", status: "ok"]))
                        } else {
                            ws.writeTextFrame(Json.encode(reply.body()))
                        }
                    }
                    break
                case ~/findAll/:
                    eb.send("messages.find", [:]) { Message message ->
                        ws.writeTextFrame(Json.encode([command: "findAll", result: message.body().results]))
                    }
                    break
                default:
                    ws.writeTextFrame(Json.encode([status: "error", message: "command not found"]))
                    break
            }
        }
    } else {
        ws.reject()
    }
}

// start server
server.listen(container.env['PORT']?.toString()?.toInteger() ?: 9000)
