#!/usr/bin/env groovy

import groovy.servlet.AbstractHttpServlet
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import static java.lang.System.getenv

DAEMON_PORT = 3000

@Grapes([
        @Grab(group='javax.servlet', module='javax.servlet-api', version='3.0.1'),
        @Grab(group='org.eclipse.jetty.aggregate', module='jetty-all-server', version='8.1.15.v20140411', transitive=false)
])

def startJetty() {
    def server = new Server(DAEMON_PORT)
    def contextHandler = new ServletContextHandler(server, '/')
    contextHandler.resourceBase = '.'
    contextHandler.addServlet(HomeServlet, '/')
    contextHandler.addServlet(DownloadServlet, '/dl')
    server.start()
}

class HomeServlet extends AbstractHttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.writer.print """\
        <html><body>
        Drag and drop the link below to your bookmarks bar<br><br>
        <a href='javascript:(function(){
            var u=encodeURIComponent(location.href);
            window.open("http://$req.localName:$req.localPort/dl?url="+u, "dl_popup", "height=800px,width=600px,resizable=1,alwaysRaised=1");})();'>youtube-dl</a>
        </body></html>
        """.stripIndent()
    }
}

class DownloadServlet extends AbstractHttpServlet {

    def DOWNLOAD_DIR = "${getenv('HOME')}/Downloads/"

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        def url = req.getParameter('url')
        def process = "youtube-dl --no-playlist -o $DOWNLOAD_DIR/%(title)s.%(ext)s --extract-audio $url".execute()

        process.waitFor()

        resp.writer.print process.in.text
        resp.writer.print process.err.text
    }
}

println "Starting youtube-dl daemon, press Ctrl+C to stop."
this.startJetty()