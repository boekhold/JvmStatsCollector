import groovy.transform.TupleConstructor

@TupleConstructor
class JvmEvent {
    String name
    Date timestamp
    long size
    long used
}
