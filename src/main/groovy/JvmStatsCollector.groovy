import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class JvmStatsCollector {
    private static Logger LOG = LoggerFactory.getLogger(JvmStatsCollector.class)

    static void main(String[] args) {
        String appdir = System.properties['app.dir']
        def conffile = new File((String)appdir, 'JvmStatsCollector.config')

        def config = null
        LOG.info("reading config from '{}'", conffile)
        config = new ConfigSlurper().parse(conffile.toURL())

        //noinspection GroovyAssignabilityCheck
        DbServer dbServer = new DbServer(
            config.db.tcpServerOptions,
            config.db.webServerOptions
        )
        //noinspection GroovyAssignabilityCheck
        DbPersister persister = new DbPersister(
            dbServer,
            config.db.location,
            config.db.username,
            config.db.password
        )

        def sampleInterval = config.sampleInterval
        def monitors = []
        config.monitors.each { name, setup ->
            //noinspection GroovyAssignabilityCheck
            def monitor = JvmMonitor.getJvmMonitor(
                name,
                setup.cmdLinePatterns,
                setup.jvmArgsPatterns
            )
            monitor.addStatListener(persister)
            monitors << monitor
        }

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5)
        monitors.each { monitor ->
            //noinspection GroovyAssignabilityCheck
            scheduledExecutorService.scheduleAtFixedRate(monitor, 0, sampleInterval, TimeUnit.SECONDS)
        }
    }
}

