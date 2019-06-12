package com.plugin.savetofile

import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.workflow.DataOutput
import com.dtolabs.rundeck.core.logging.LogEventControl
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.PluginLoggingContext
import spock.lang.Specification

class SaveToFileSpec extends Specification {

//    def "test preset type "() {
//        given:
//        def plugin = new SaveToFile()
//        plugin.datatype = datatype
//        plugin.header = "test"
//        def sharedoutput = new DataOutput(ContextView.global())
//        def context = Mock(PluginLoggingContext) {
//            getOutputContext() >> sharedoutput
//        }
//        def events = []
//        lines.each { line ->
//            events << Mock(LogEventControl) {
//                getMessage() >> line
//                getEventType() >> 'log'
//                getLoglevel() >> LogLevel.NORMAL
//            }
//        }
//        when:
//        plugin.init(context)
//        events.each {
//            plugin.handleEvent(context, it)
//        }
//        plugin.complete(context)
//
//        then:
//        1 * context.log(2, output, meta)
//
//        where:
//        datatype   | lines                             | output                | meta
//        'text/plain' | ['1,2,3', '---', 'a,b,c']         | '[test] 1,2,3\n[test] ---\n[test] a,b,c\n' | ['content-data-type': 'text/plain']
//        'text/html' | ['1,2,3', '---', 'a,b,c']         | "<table class='table table-striped'><tr><th>Log Output</th></tr><tr><td><b>[test]</b> 1,2,3</td></tr><tr><td><b>[test]</b> ---</td></tr><tr><td><b>[test]</b> a,b,c</td></tr></table>" | ['content-data-type': 'text/html']
//    }

    def "Test write to file no node information"() {
        setup:
        File tmpDest = File.createTempFile("log","test")
        SaveToFile saveToFile = new SaveToFile()
        saveToFile.fileDestination = tmpDest.absolutePath
        saveToFile.appendToFile = false
        def sharedoutput = new DataOutput(ContextView.global())
        def dataContext = DataContextUtils.context()
        def context = Mock(PluginLoggingContext) {
            getOutputContext() >> sharedoutput
            getDataContext() >> dataContext
        }
        def events = []
        events << createMockEvent("Log event 1")
        events << createMockEvent("Log event 2")

        when:
        saveToFile.init(context)
        events.each {
            saveToFile.handleEvent(context,it)
        }
        saveToFile.complete(context)
        def output = tmpDest.newReader().readLines()

        then:
        output[0] == "Log event 1"
        output[1] == "Log event 2"
    }

    def "Test write to file with node name replacement"() {
        setup:
        File tmpDestDir = File.createTempDir()
        SaveToFile saveToFile = new SaveToFile()
        saveToFile.fileDestination = tmpDestDir.absolutePath+'/${node.name}.txt'
        saveToFile.appendToFile = false
        def sharedoutput = new DataOutput(ContextView.global())
        def dataContext = DataContextUtils.context("node",["name":"testnode"])
        def sharedDataContext = SharedDataContextUtils.sharedContext()
        sharedDataContext.merge(ContextView.node(),dataContext)
        def context = Mock(PluginLoggingContext) {
            getOutputContext() >> sharedoutput
            getDataContext() >> dataContext
            getSharedDataContext() >> sharedDataContext
        }
        def events = []
        events << createMockEvent("Log event 1")
        events << createMockEvent("Log event 2")

        when:
        saveToFile.init(context)
        events.each {
            saveToFile.handleEvent(context,it)
        }
        saveToFile.complete(context)
        def output = new File(tmpDestDir,"testnode.txt").newReader().readLines()

        then:
        output[0] == "Log event 1"
        output[1] == "Log event 2"

    }

    def "Test append to file"() {
        setup:
        File tmpDest = File.createTempFile("log","test")
        SaveToFile saveToFile1 = new SaveToFile()
        saveToFile1.fileDestination = tmpDest.absolutePath
        saveToFile1.appendToFile = false
        SaveToFile saveToFile2 = new SaveToFile()
        saveToFile2.fileDestination = tmpDest.absolutePath
        saveToFile2.appendToFile = true
        def sharedoutput = new DataOutput(ContextView.global())
        def dataContext = DataContextUtils.context()
        def context = Mock(PluginLoggingContext) {
            getOutputContext() >> sharedoutput
            getDataContext() >> dataContext
        }
        def events = []
        events << createMockEvent("Log event 1")
        events << createMockEvent("Log event 2")

        when:
        saveToFile1.init(context)
        events.each { saveToFile1.handleEvent(context,it) }
        saveToFile1.complete(context)
        saveToFile2.init(context)
        events.each { saveToFile2.handleEvent(context,it) }
        saveToFile2.complete(context)
        def output = tmpDest.newReader().readLines()

        then:
        output[0] == "Log event 1"
        output[1] == "Log event 2"
        output[2] == "Log event 1"
        output[3] == "Log event 2"
    }

    private LogEventControl createMockEvent(String msg) {
        Mock(LogEventControl) {
            getMessage() >> msg
            getEventType() >> 'log'
            getLoglevel() >> LogLevel.NORMAL
        }
    }
}