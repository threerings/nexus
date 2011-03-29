//
// $Id$

package nexus.chat.web;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;

import com.samskivert.nexus.client.GWTClient;
import com.samskivert.nexus.client.NexusClient;

/**
 * The main entry point for the GWT chat app.
 */
public class ChatEntryPoint implements EntryPoint
{
    // from interface EntryPoint
    public void onModuleLoad ()
    {
        WebContext ctx = new WebContext() {
            public NexusClient getClient () {
                return _client;
            }
            public void setMainPanel (Panel panel) {
                if (_main != null) {
                    RootPanel.get(CLIENT_DIV).remove(_main);
                }
                RootPanel.get(CLIENT_DIV).add(_main = panel);
            }
            protected NexusClient _client = GWTClient.create(new ChatSerializer());
            protected Panel _main;
        };
        ctx.setMainPanel(new ConnectPanel(ctx));
    }

    protected static final String CLIENT_DIV = "client";
}