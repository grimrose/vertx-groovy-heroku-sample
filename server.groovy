import org.vertx.groovy.core.buffer.Buffer
import org.vertx.groovy.core.http.HttpServerRequest
import org.vertx.groovy.core.http.RouteMatcher
import org.vertx.groovy.core.http.ServerWebSocket

RouteMatcher matcher = new RouteMatcher()
matcher.get("/") { request ->
    request.response.sendFile('index.html')
}

matcher.noMatch { HttpServerRequest request ->
    request.response.setStatusCode(404)
    request.response.end()
}

vertx.createHttpServer().requestHandler(matcher.asClosure()).websocketHandler { ServerWebSocket ws ->
    if (ws.path == '/ws') {
        ws.dataHandler { Buffer data ->
            ws.writeTextFrame(data.toString())
        }
    } else {
        ws.reject()
    }
}.listen(9000)
