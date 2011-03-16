//
// $Id$

package nexus.chat.client;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

/**
 * Displays an interface for connecting to a chat server.
 */
public class ConnectPanel extends JPanel
{
    public ConnectPanel (ChatContext ctx)
    {
        _ctx = ctx;

        VGroupLayout layout = new VGroupLayout();
        layout.setOffAxisPolicy(VGroupLayout.EQUALIZE);
        setLayout(layout);
        add(new JLabel("Server address:"));

        JPanel row = GroupLayout.makeHBox();
        final JTextField address = new JTextField() {
            // for fuck's sake Swing, really?
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width = Math.max(200, d.width);
                return d;
            }
        };
        row.add(address);

        JButton connect = new JButton("Connect");
        row.add(connect, GroupLayout.FIXED);
        add(row);

        ActionListener onConnect = new ActionListener() {
            public void actionPerformed (ActionEvent e) {
                onConnect(address.getText().trim());
            }
        };
        address.addActionListener(onConnect);
        connect.addActionListener(onConnect);
    }

    protected void onConnect (String address)
    {
        System.out.println("TODO: connect to " + address);
        // TODO: _ctx.getClient().subscribe(Address.make(address, ChatObject.class), new
        // Callback<ChatObject>() { ... });
    }

    protected ChatContext _ctx;
}
