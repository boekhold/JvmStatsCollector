import org.junit.Ignore
import org.junit.Test
import sun.jvmstat.monitor.HostIdentifier
import sun.jvmstat.monitor.IntegerMonitor
import sun.jvmstat.monitor.LongMonitor
import sun.jvmstat.monitor.MonitoredHost
import sun.jvmstat.monitor.MonitoredVm
import sun.jvmstat.monitor.StringMonitor
import sun.jvmstat.monitor.VmIdentifier

class MonitoredHostTest {
    @Ignore
    @Test
    void listAllMonitors() {
        HostIdentifier thisHostId = new HostIdentifier((String)null)
        MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost(thisHostId)

        monitoredHost.activeVms().each { id ->
            // 'id' is the OS PID...
            println "ID: $id --------------------------------------------------------------------------"

            VmIdentifier jvmId = new VmIdentifier("//${id}?mode=r")
            MonitoredVm vm = monitoredHost.getMonitoredVm(jvmId)

            vm.findByPattern('.*').each { monitor ->
                if (monitor instanceof StringMonitor)
                    println "S: ${monitor.name} : ${monitor.stringValue()}"
                if (monitor instanceof IntegerMonitor)
                    println "I: ${monitor.name} : ${monitor.intValue()}"
                if (monitor instanceof LongMonitor)
                    println "L: ${monitor.name} : ${monitor.longValue()}"
            }
        }
    }

    @Ignore
    @Test
    void monitoredHostTest() {
        HostIdentifier thisHostId = new HostIdentifier((String)null)
        MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost(thisHostId)

        monitoredHost.activeVms().each { id ->
            // 'id' is the OS PID...
            println "ID: $id --------------------------------------------------------------------------"

            VmIdentifier jvmId = new VmIdentifier("//${id}?mode=r")
            MonitoredVm vm = monitoredHost.getMonitoredVm(jvmId)

            /**
             * Interesting "monitors" useful for identifying a specific JVM instance are:
             * - java.rt.vmArgs
             * - sun.rt.javaCommand
             */

            println getJavaCommand(vm)
            println getJvmArgs(vm)
            println "Total Allocated : ${getTotalAlloc(vm)}"
            println "Total Used      : ${getTotalUsed(vm)}"
        }
    }

    @Ignore
    @Test
    void monitorSinglePID() {
        HostIdentifier thisHostId = new HostIdentifier((String)null)
        MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost(thisHostId)

        VmIdentifier jvmId = new VmIdentifier("//248?mode-r")
        MonitoredVm vm = monitoredHost.getMonitoredVm(jvmId)

        println getJavaCommand(vm)
        println getJvmArgs(vm)

        println "---------------"

        (0..50).each {
            println "Total Allocated : ${getTotalAlloc(vm)}"
            println "Total Used      : ${getTotalUsed(vm)}"
            println "---------------"
            sleep 5000
        }
    }

    static String getJavaCommand(MonitoredVm vm) {
        vm.findByName('sun.rt.javaCommand').stringValue()
    }

    static String getJvmArgs(MonitoredVm vm) {
        vm.findByName('java.rt.vmArgs').stringValue()
    }

    static long getTotalAlloc(MonitoredVm vm) {
        /**
         * Pay attention:
         *
         * youngAlloc + OldAlloc != edenAlloc + survivor0Alloc + survivor1Alloc + tenuredAlloc
         *
         * Compare with the way JVisualVM reports "Size" under the "Heap" tab. If you add the
         * individual numbers, they don't match.
         */
        return getYoungAlloc(vm) + getOldAlloc(vm)
    }

    static long getTotalUsed(MonitoredVm vm) {
        return getEdenUsed(vm) +
            getSurvivor0Used(vm) +
            getSurvivor1Used(vm) +
            getTenuredUsed(vm)
    }

    static long getYoungAlloc(MonitoredVm vm) {
        vm.findByName('sun.gc.generation.0.capacity').longValue()
    }
    
    static long getEdenAlloc(MonitoredVm vm) {
        vm.findByName('sun.gc.generation.0.space.0.capacity').longValue()
    }

    static long getEdenUsed(MonitoredVm vm) {
        vm.findByName('sun.gc.generation.0.space.0.used').longValue()
    }

    static long getSurvivor0Alloc(MonitoredVm vm) {
        vm.findByName('sun.gc.generation.0.space.1.capacity').longValue()
    }

    static long getSurvivor0Used(MonitoredVm vm) {
        vm.findByName('sun.gc.generation.0.space.1.used').longValue()
    }

    static long getSurvivor1Alloc(MonitoredVm vm) {
        vm.findByName('sun.gc.generation.0.space.2.capacity').longValue()
    }

    static long getSurvivor1Used(MonitoredVm vm) {
        vm.findByName('sun.gc.generation.0.space.2.used').longValue()
   }

    static long getOldAlloc(MonitoredVm vm) {
        vm.findByName('sun.gc.generation.1.capacity').longValue()
    }

    static long getTenuredAlloc(MonitoredVm vm) {
        vm.findByName('sun.gc.generation.1.space.0.capacity').longValue()
    }

    static long getTenuredUsed(MonitoredVm vm) {
        vm.findByName('sun.gc.generation.1.space.0.used').longValue()
    }
}
