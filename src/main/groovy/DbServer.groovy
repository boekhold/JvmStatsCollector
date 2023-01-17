import org.h2.tools.Server

class DbServer {
    Server dbServer
    Server webServer

    DbServer(String dbArgs, String wsArgs) {
        dbServer = Server.createTcpServer(dbArgs.split('\\s+'))
        webServer = Server.createWebServer(wsArgs.split('\\s+'))

        dbServer.start()
        webServer.start()
    }

    String getUrl() {
        dbServer.getURL()
    }

    void stop() {
        webServer?.stop()
        dbServer?.stop()
    }
}
