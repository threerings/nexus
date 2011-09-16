//
// Nexus Chat Demo - demonstrates Nexus with some chattery
// http://github.com/threerings/nexus/blob/master/LICENSE

package nexus.chat.web;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.Widgets;

import com.threerings.nexus.distrib.Address;
import com.threerings.nexus.util.Callback;

import nexus.chat.distrib.ChatObject;

/**
 * Displays the UI for connecting to a chat server.
 */
public class ConnectPanel extends FlowPanel
{
    public ConnectPanel (WebContext ctx) {
        _ctx = ctx;

        final TextBox address = Widgets.newTextBox("", -1, 40);
        final Button connect = new Button("Connect");
        add(Widgets.newRow(Widgets.newLabel("Server address:"), address, connect));

        add(_status = Widgets.newLabel(""));

        ClickHandler onConnect = new ClickHandler() {
            public void onClick (ClickEvent event) {
                onConnect(address.getText().trim());
            }
        };
        EnterClickAdapter.bind(address, onConnect);
        connect.addClickHandler(onConnect);

        // try connecting to localhost straight away
        address.setText(Window.Location.getHostName());
        onConnect.onClick(null);
    }

    protected void onConnect (String address) {
        _status.setText("Connecting to " + address + "...");

        // subscribe to the singleton ChatObject on the specified host; this will trigger a
        // connection to that host
        _ctx.getClient().subscribe(
            Address.create(address, ChatObject.class), new Callback<ChatObject>() {
            public void onSuccess (ChatObject chatobj) {
                // we're connected, switch to the main chat display
                _ctx.setMainPanel(new ChatPanel(_ctx, chatobj));
            }
            public void onFailure (Throwable cause) {
                _status.setText("Failed to connect: " + cause.getMessage());
            }
        });
    }

    protected WebContext _ctx;
    protected Label _status;
}
