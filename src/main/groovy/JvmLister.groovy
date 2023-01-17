import sun.jvmstat.monitor.HostIdentifier
import sun.jvmstat.monitor.MonitoredHost
import sun.jvmstat.monitor.MonitoredVm
import sun.jvmstat.monitor.VmIdentifier

HostIdentifier thisHostId = new HostIdentifier((String)null)
MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost(thisHostId)

monitoredHost.activeVms().each { id ->
    VmIdentifier jvmId = new VmIdentifier("//${id}?mode=r")
    MonitoredVm vm = monitoredHost.getMonitoredVm(jvmId)

    println ">>>>>>>>>"
    println "PID: ${vm.vmIdentifier.localVmId}"
    println "command: " + vm.findByName('sun.rt.javaCommand').stringValue()
    println "vm args: " + vm.findByName('java.rt.vmArgs').stringValue()
}
