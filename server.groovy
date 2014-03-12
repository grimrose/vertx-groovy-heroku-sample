import org.vertx.groovy.core.buffer.Buffer
import org.vertx.groovy.core.http.HttpServerRequest
import org.vertx.groovy.core.http.RouteMatcher
import org.vertx.groovy.core.http.ServerWebSocket
import org.vertx.groovy.platform.Verticle

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
            def message = data.toString()
            container.logger.debug(message)
            ws.writeTextFrame(message)
        }
    } else {
        ws.reject()
    }
}.listen(container.env['PORT']?.toString()?.toInteger()?: 9000)
