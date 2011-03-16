//
// $Id$

package nexus.chat.client;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPanel;

/**
 * Displays an interface for connecting to a chat server.
 */
public class ConnectPanel extends JPanel
{
    public ConnectPanel (ChatContext ctx)
    {
        add(new JLabel("Server address:"));
        add(new JTextField());
    }
}
