#!/usr/bin/env groovy

import groovy.json.JsonOutput
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.ErrorHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.PermittedOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler

import static java.lang.System.getenv

DAEMON_PORT = 3001
DOWNLOAD_DIR = "${getenv('HOME')}/Downloads/"

vertx = Vertx.vertx()

@Grapes([
        @Grab(group='io.vertx', module='vertx-core', version='3.1.0'),
        @Grab(group='io.vertx', module='vertx-web', version='3.1.0')
])

def startVertx() {
    println 'Starting Vert.x'
    def router = Router.router(vertx)
    router.route('/eventbus/*').handler(eventBusHandler())

    router.getWithRegex('\\/dl\\/(.*)').handler(downloadRequestHandler)

    router.route().failureHandler(ErrorHandler.create())
    router.route().handler(StaticHandler.create())

    vertx.createHttpServer().requestHandler(router.&accept).listen(DAEMON_PORT)
}

downloadRequestHandler = { context ->
    def url = context.request().getParam('param0')
    println "Requested download of URL: $url"

    def process = "youtube-dl --no-playlist -o $DOWNLOAD_DIR/%(title)s.%(ext)s --extract-audio $url".execute()

    asyncReader(process.in, 'stdin')
    asyncReader(process.err, 'stderr')

    context.response()
            .putHeader('Location', '../status.html')
            .setStatusCode(HttpURLConnection.HTTP_SEE_OTHER)
            .end()
}

SockJSHandler eventBusHandler() {
    def options = new BridgeOptions()
            .addOutboundPermitted(new PermittedOptions().setAddress('output_feed')
    )
    SockJSHandler.create(vertx).bridge(options)
}

def asyncReader(stream, address) {
    Thread.startDaemon {
        def reader = new BufferedReader(new InputStreamReader(stream))

        def line
        while (line = reader.readLine()) {
            println line
            vertx.eventBus().send('output_feed', JsonOutput.toJson([line: line]))
        }

        println "$address reader finished"
    }
}

println "Starting youtube-dl daemon, press Ctrl+C to stop."
startVertx()