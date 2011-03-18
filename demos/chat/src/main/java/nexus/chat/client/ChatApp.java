//
// $Id$

package nexus.chat.client;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.samskivert.nexus.client.JVMClient;
import com.samskivert.nexus.client.NexusClient;

/**
 * The main entry point for the chat demo client.
 */
public class ChatApp
{
    public static void main (String[] args)
    {
        final JFrame frame = new JFrame("Chat Demo");
        ChatContext ctx = new ChatContext() {
            public NexusClient getClient () {
                return _client;
            }
            public void setMainPanel (JPanel panel) {
                if (_main != null) {
                    frame.remove(_main);
                }
                frame.add(_main = panel, BorderLayout.CENTER);
            }
            protected NexusClient _client = JVMClient.create(47624);
            protected JPanel _main;
        };

        ctx.setMainPanel(new ConnectPanel(ctx));
        frame.setSize(800, 600);
        frame.setVisible(true);
    }
}
