//
// Nexus Chat Demo - demonstrates Nexus with some chattery
// http://github.com/threerings/nexus/blob/master/LICENSE

package nexus.chat.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.threerings.nexus.distrib.Address;
import com.threerings.nexus.util.Callback;
import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import nexus.chat.distrib.ChatObject;

/**
 * Displays an interface for connecting to a chat server.
 */
public class ConnectPanel extends JPanel
{
    public ConnectPanel (ChatContext ctx) {
        _ctx = ctx;

        VGroupLayout layout = new VGroupLayout();
        layout.setOffAxisPolicy(VGroupLayout.EQUALIZE);
        setLayout(layout);
        add(new JLabel("Server address:"));

        JPanel row = GroupLayout.makeHBox();
        final JTextField address = UIUtil.newTextField(200);
        row.add(address);

        JButton connect = new JButton("Connect");
        row.add(connect, GroupLayout.FIXED);
        add(row);

        add(_status = new JTextArea());

        ActionListener onConnect = new ActionListener() {
            public void actionPerformed (ActionEvent e) {
                onConnect(address.getText().trim());
            }
        };
        address.addActionListener(onConnect);
        connect.addActionListener(onConnect);

        // try connecting to localhost straight away
        address.setText("localhost");
        onConnect.actionPerformed(null);
    }

    protected void onConnect (String address) {
        _status.setText("Connecting to " + address + "...");

        // subscribe to the singleton ChatObject on the specified host; this will trigger a
        // connection to that host
        _ctx.getClient().subscribe(
            Address.create(address, ChatObject.class), new Callback<ChatObject>() {
            public void onSuccess (ChatObject chatobj) {
                System.err.println("Got chat object " + chatobj);
                // we're connected, switch to the main chat display
                _ctx.setMainPanel(new ChatPanel(_ctx, chatobj));
            }
            public void onFailure (Throwable cause) {
                _status.setText("Failed to connect: " + cause.getMessage());
            }
        });
    }

    protected ChatContext _ctx;
    protected JTextArea _status;
}
