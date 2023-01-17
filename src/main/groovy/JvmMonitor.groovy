import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sun.jvmstat.monitor.*
import sun.jvmstat.monitor.event.HostEvent
import sun.jvmstat.monitor.event.HostListener
import sun.jvmstat.monitor.event.VmStatusChangeEvent

class JvmMonitor implements HostListener, Runnable {
    private static Logger LOG = LoggerFactory.getLogger(JvmMonitor.class)

    private static HostIdentifier thisHostId = new HostIdentifier((String)null)
    private static MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost(thisHostId)

    private MonitoredVm vm

    private String name
    private List<String> commandLinePatterns
    private List<String> vmArgsPatterns

    private List<StatListener> listeners = new LinkedList<>()

    private JvmMonitor() {
    }

    def stop() {
        vm?.detach()
    }

    static JvmMonitor getJvmMonitor(String name, List<String> commandLinePatterns, List<String> vmArgsPatterns) {
        JvmMonitor jvmMonitor = new JvmMonitor()
        jvmMonitor.name = name
        jvmMonitor.commandLinePatterns = commandLinePatterns
        jvmMonitor.vmArgsPatterns = vmArgsPatterns
        monitoredHost.addHostListener(jvmMonitor)

        return jvmMonitor
    }

    private static MonitoredVm getMonitoredVm(List<String> commandLinePatterns, List<String> vmArgsPatterns) {
        def vm = monitoredHost.activeVms().findResult { id ->
            VmIdentifier jvmId = new VmIdentifier("//${id}?mode=r")
            MonitoredVm vm = monitoredHost.getMonitoredVm(jvmId)

            def javaCommandLine = vm.findByName('sun.rt.javaCommand').stringValue()
            def vmArgs = vm.findByName('java.rt.vmArgs').stringValue()

            def match = commandLinePatterns.every { pattern ->
                javaCommandLine =~ pattern
            }
            if (match) {
                match = vmArgsPatterns.every { pattern ->
                    vmArgs =~ pattern
                }
            }

            return match ? vm : null
        }
        //println "getMonitoredVm: ${vm ? vm.vmIdentifier.localVmId : 'n/a'}"
        return vm
    }

    void addStatListener(StatListener listener) {
        listeners << listener
    }

    @Override
    void run() {
        def youngSize = findByName('sun.gc.generation.0.capacity')?.longValue()
        def oldSize   = findByName('sun.gc.generation.1.capacity')?.longValue()

        def edenUsed = findByName('sun.gc.generation.0.space.0.used')?.longValue()
        def s0Used   = findByName('sun.gc.generation.0.space.1.used')?.longValue()
        def s1Used   = findByName('sun.gc.generation.0.space.2.used')?.longValue()
        def oldUsed  = findByName('sun.gc.generation.1.space.0.used')?.longValue()

        // Assumption: if we could get "youngSize" there's no reason we couldn't get
        // the other values as well.
        // Can't use groovy-truth here because youngSize could be zero, which fails
        // groovy-truth.
        if (youngSize != null) {
            //noinspection GroovyAssignabilityCheck
            def evt = new JvmEvent(
                name,
                new Date(),
                youngSize+oldSize,
                edenUsed+s0Used+s1Used+oldUsed
            )

            listeners.each { listener ->
                listener.onStatEvent(evt)
            }
        }
    }

    Monitor findByName(name) throws MonitorException {
        if (vm)
            return vm.findByName(name)

        // not connected yet, or disconnected
        vm = getMonitoredVm(commandLinePatterns, vmArgsPatterns)
        if (vm) {
            LOG.info('JvmMonitor \'{}\': detected host PID {}', this.name, vm.vmIdentifier.localVmId)
        }
        return vm?.findByName(name)
    }

    List<Monitor> findByPattern(pattern) throws MonitorException {
        if (vm)
            return vm.findByPattern(pattern)

        // not connected yet, or disconnected
        vm = getMonitoredVm(commandLinePatterns, vmArgsPatterns)
        if (vm) {
            LOG.info('JvmMonitor \'{}\': detected host PID {}', this.name, vm.vmIdentifier.localVmId)
        }
        return vm?.findByPattern(pattern)
    }

    @Override
    void vmStatusChanged(VmStatusChangeEvent vmStatusChangeEvent) {
        if (vm && vmStatusChangeEvent.terminated.contains(vm.vmIdentifier.localVmId as Integer)) {
            // our VM has died, need to explicitly clean up, because the current
            // vm object seems to remain valid if we don't do this!
            vm.detach()
            vm = null
        }
    }

    @Override
    void disconnected(HostEvent hostEvent) {
        // we're monitoring only the local host, should never
        // get disconnected
    }
}
