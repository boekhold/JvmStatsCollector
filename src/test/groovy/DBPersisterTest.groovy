import groovy.sql.Sql
import org.junit.Ignore
import org.junit.Test

class DBPersisterTest {
    @Test
    void testDbCreation() {
        Sql.withInstance(
            'jdbc:h2:mem:test;INIT=RUNSCRIPT FROM \'classpath:/schema.sql\''
            ,'rknet'
            ,'rknet12'
            ,'org.h2.Driver') { sql ->
            sql.execute("INSERT INTO JVM_STATS VALUES (?, ?, ?, ?)", 'dummy', new Date(), 1, 2)
            def row = sql.firstRow('SELECT * FROM JVM_STATS')
            assert row.NAME == 'dummy'
            assert row.SIZE == 1
            assert row.USED == 2
        }
    }

    @Test
    void basicEventInsert() {
        DbServer dbServer = new DbServer(
            '-tcpAllowOthers -tcpPort 9092',
            '-webAllowOthers -webPort 9093'
        )
        println dbServer.url
        def db = new DbPersister(dbServer, 'mem:test', 'sa', 'sa12')
        def evt = new JvmEvent('dummy', new Date(), 2, 1)

        db.onStatEvent(evt)
        evt.timestamp = new Date()
        db.onStatEvent(evt)
        evt.timestamp = new Date()
        db.onStatEvent(evt)

        def row = db.conn.firstRow('SELECT COUNT(*) CNT FROM JVM_STATS')
        assert row.CNT == 3
        row = db.conn.firstRow('SELECT USED FROM JVM_STATS')
        assert row.USED == 1
    }

    @Ignore
    @Test
    void manuallyTestEmbeddedWebServer() {
        basicEventInsert()
        sleep 600*1000 // you'll have 10 minutes to test!
    }
}
