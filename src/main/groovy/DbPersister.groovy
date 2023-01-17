import groovy.sql.Sql
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DbPersister implements StatListener {
    private static Logger LOG = LoggerFactory.getLogger(DbPersister.class)

    private static String INSERT_STATEMENT =
        'INSERT INTO JVM_STATS (NAME, STAMP, SIZE, USED) VALUES (?,?,?,?)'

    Sql conn

    DbPersister(DbServer dbServer, String dbLoc, String usr, String pwd) {
        def url = "jdbc:h2:${dbServer.url}/${dbLoc};INIT=RUNSCRIPT FROM 'classpath:/schema.sql'"
        conn = Sql.newInstance(url, usr, pwd, 'org.h2.Driver')
        conn.cacheStatements = true

        LOG.info('Connected to DB on {}', "jdbc:h2:${dbServer.url}/${dbLoc}")
    }

    @Override
    void onStatEvent(JvmEvent evt) {
        def params = [evt.name, evt.timestamp, evt.size, evt.used]
        conn.execute(INSERT_STATEMENT, params)
    }
}
