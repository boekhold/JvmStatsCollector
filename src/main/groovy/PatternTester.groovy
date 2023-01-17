if (args.size() != 2) {
    println '''
Usage: PatternTester <commandLinePatterns> <vmArgsPatterns>

Each <...Patterns> option is a single string with multiple
patterns separated by '##', ie:

PatternTester "Patt##.+er" "-Dso##.+ing"

This specifies 2 patterns for the java command that *both* have
to match, and 2 patterns for the VM arguments that *both* have
to match.
'''
}
List<String> cmdPatterns = args[0].split('##')
List<String> argPatterns = args[1].split('##')

def vm = JvmMonitor.getJvmMonitor('', cmdPatterns, argPatterns)

if (vm.findByName('sun.rt.javaCommand')) {
    println "Found VM with PID ${vm.vm.vmIdentifier.localVmId}"
    println "command: " + vm.findByName('sun.rt.javaCommand').stringValue()
    println "vm args: " + vm.findByName('java.rt.vmArgs').stringValue()
} else {
    println "Did not find a VM using specified patterns"
}
