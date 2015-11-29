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

DAEMON_PORT = 3000
DOWNLOAD_DIR = "${getenv('HOME')}/Downloads/"

vertx = Vertx.vertx()

@Grapes([
        @Grab(group='io.vertx', module='vertx-core', version='3.1.0'),
        @Grab(group='io.vertx', module='vertx-web', version='3.1.0')
])

def startVertx() {
    println 'Starting Vert.x'
    def router = Router.router(vertx)

    // Create a bridge/event bus extension for SockJS
    // The event bus is extended to be available on the web side
    router.route('/eventbus/*').handler(eventBusHandler())

    // Handle GET requests on /dl/{url}
    router.getWithRegex('\\/dl\\/(.*)').handler(downloadRequestHandler)

    // Generate nice error pages on error
    router.route().failureHandler(ErrorHandler.create())

    // Serve static content from /webroot
    router.route().handler(StaticHandler.create())

    vertx.createHttpServer().requestHandler(router.&accept).listen(DAEMON_PORT)
}

SockJSHandler eventBusHandler() {
    // Vertx defaults to a deny-all policy on SockJS bridge so we need to set
    // the allowed addresses
    def options = new BridgeOptions()
            .addOutboundPermitted(new PermittedOptions().setAddressRegex('feed\\..*')
    )
    SockJSHandler.create(vertx).bridge(options)
}

downloadRequestHandler = { context ->
    // param0 is the first regex capture group from the URI
    def url = context.request().getParam('param0')
    println "Requested download of URL: $url"

    def process = "youtube-dl --no-playlist -o $DOWNLOAD_DIR/%(title)s.%(ext)s --extract-audio $url".execute()

    // Asynchronously read from both streams and publish the result on the event bus
    // on the appropriate address
    asyncReader(process.in, 'feed.output')
    asyncReader(process.err, 'feed.error')

    // Download initiated, redirect to the status page
    context.response()
            .putHeader('Location', '../status.html')
            .setStatusCode(HttpURLConnection.HTTP_SEE_OTHER)
            .end()
}

def asyncReader(stream, address) {
    Thread.startDaemon {
        def reader = new BufferedReader(new InputStreamReader(stream))

        def line
        while (line = reader.readLine()) {
            println line
            vertx.eventBus().send(address, JsonOutput.toJson([line: line]))
        }

        println "$address reader finished"
    }
}

println "Starting youtube-dl daemon, press Ctrl+C to stop."
startVertx()