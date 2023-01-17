import org.junit.Test

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class JvmMonitorTest {
    private static String VERSION_PROP = 'java.property.java.vm.specification.version'
    @Test
    void findJUnitTesterVM() {
        JvmMonitor vm = JvmMonitor.getJvmMonitor(
            'JUnit',
            ['JvmMon.+Test', 'f.*TesterVM'],
            ['-D']
        )

        assert vm.findByName(VERSION_PROP).stringValue() == '1.8'
    }

    @Test
    void testJvmStartedLater() {
        JvmMonitor vm = JvmMonitor.getJvmMonitor(
            'JUnit',
            ['BackgroundJvmMain'],
            ['-Dsom.*ing']
        )

        assert vm.findByName(VERSION_PROP) == null

        def subVm = launchJvm('BackgroundJvmMain', ['-Dsomething'])

        sleep 2000
        assert vm.findByName(VERSION_PROP).stringValue() == '1.8'
        subVm.destroy()
    }

    @Test
    void testJvmRestarted() {
        JvmMonitor vm = JvmMonitor.getJvmMonitor(
            'JUnit',
            ['BackgroundJvmMain'],
            ['-Dsom.*ing']
        )

        def subVm = launchJvm('BackgroundJvmMain', ['-Dsomething'])
        sleep 2000
        assert vm.findByName(VERSION_PROP).stringValue() == '1.8'
        subVm.destroy()
        // sleep more than the default interval of 1000ms of MonitoredHost/MonitoredVm
        sleep 1100

        assert vm.findByName(VERSION_PROP) == null

        subVm = launchJvm('BackgroundJvmMain', ['-Dsomething'])
        sleep 2000
        assert vm.findByName(VERSION_PROP).stringValue() == '1.8'
        subVm.destroy()
    }

    @Test
    void testRunnableAndListeners() {
        JvmMonitor vm = JvmMonitor.getJvmMonitor(
            'JUnit',
            ['BackgroundJvmMain'],
            ['-Dsom.*ing']
        )

        def subVm = launchJvm('BackgroundJvmMain', ['-Dsomething'])
        sleep 2000

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2)
        def events = []
        vm.addStatListener( new StatListener() {
            @Override
            void onStatEvent(JvmEvent evt) {
                events << evt
                if (events.size() == 5)
                    scheduledExecutorService.shutdown()
            }
        })
        scheduledExecutorService.scheduleAtFixedRate(vm, 0, 1, TimeUnit.SECONDS)
        scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS)
        subVm.destroy()

        assert events.size() == 5
        assert events[0].name == 'JUnit'
    }

    private static Process launchJvm(String clazzName, List<String> vmArgs) {
        def sep = System.properties['file.separator']
        def cmd = [System.properties['java.home'] + sep + 'bin' + sep + 'java']
        cmd += '-cp'
        cmd += System.properties['java.class.path']
        if (vmArgs)
            cmd += vmArgs
        cmd += clazzName

        ProcessBuilder builder = new ProcessBuilder(cmd)
        builder.redirectError(ProcessBuilder.Redirect.INHERIT)
        builder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        Process proc = builder.start()
        return proc
    }
}
