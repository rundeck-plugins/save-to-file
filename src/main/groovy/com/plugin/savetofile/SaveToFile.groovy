package com.plugin.savetofile

import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.logging.LogEventControl;
import com.dtolabs.rundeck.core.logging.LogLevel;
import com.dtolabs.rundeck.core.logging.PluginLoggingContext;
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.PluginException
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin;

@Plugin(service="LogFilter",name="SaveToFile")
@PluginDescription(title="Save To File", description="Saves logs to a file destination")
public class SaveToFile implements LogFilterPlugin{

    @PluginProperty(
            title = "Save To File",
            description = "The log data will be saved to this file",
            required = true,
            scope = PropertyScope.Instance
    )
    String fileDestination = null;

    @PluginProperty(
            title = "Append To File",
            description = "If true and a file exists it will be appended rather than overwritten",
            defaultValue = "false",
            scope = PropertyScope.Instance
    )
    boolean appendToFile = false


    private boolean       started = false;
    private File          target;
    private BufferedWriter out;

    @Override
    public void init(final PluginLoggingContext context) {
        started = true;
        def node = context.getDataContext().get("node")

        String replacedDest = SharedDataContextUtils.replaceDataReferences(
                fileDestination,
                context.sharedDataContext,
                node ? ContextView.node(node.name) : ContextView.global(),
                ContextView.&nodeStep, null, false, false
        )

        context.log(3,"writing to file: ${replacedDest}")
        target = new File(replacedDest)
        if(!target.exists()) {
            if(!target.createNewFile()) {
                throw new PluginException("Unable to create file at location: ${fileDestination}")
            }
        }
        out = target.newWriter(appendToFile)
    }

    @Override
    public void handleEvent(final PluginLoggingContext context, final LogEventControl event) {
        if(event.getEventType().equals("log") && event.getLoglevel().equals(LogLevel.NORMAL) ){
            out.append(event.getMessage()).append("\n");
        }
    }

    @Override
    public void complete(final PluginLoggingContext context) {
        if (started) {
            out.flush()
            out.close()
        }
    }
}