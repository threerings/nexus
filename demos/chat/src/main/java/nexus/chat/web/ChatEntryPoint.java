//
// Nexus Chat Demo - demonstrates Nexus with some chattery
// http://github.com/threerings/nexus/blob/master/LICENSE

package nexus.chat.web;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.nexus.client.GWTClient;
import com.threerings.nexus.client.NexusClient;

/**
 * The main entry point for the GWT chat app.
 */
public class ChatEntryPoint implements EntryPoint
{
    // from interface EntryPoint
    public void onModuleLoad () {
        WebContext ctx = new WebContext() {
            public NexusClient getClient () {
                return _client;
            }
            public void setMainPanel (Widget main) {
                if (_main != null) {
                    RootPanel.get(CLIENT_DIV).remove(_main);
                }
                RootPanel.get(CLIENT_DIV).add(_main = main);
            }
            protected NexusClient _client = GWTClient.create(6502, new ChatSerializer());
            protected Widget _main;
        };
        ctx.setMainPanel(new ConnectPanel(ctx));
    }

    protected static final String CLIENT_DIV = "client";
}
