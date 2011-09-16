//
// Nexus Chat Demo - demonstrates Nexus with some chattery
// http://github.com/threerings/nexus/blob/master/LICENSE

package nexus.chat.client;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.samskivert.util.OneLineLogFormatter;

import com.threerings.nexus.client.JVMClient;
import com.threerings.nexus.client.NexusClient;

/**
 * The main entry point for the chat demo client.
 */
public class ChatApp
{
    public static void main (String[] args) {
        // improve our logging output
        OneLineLogFormatter.configureDefaultHandler(false);

        final JFrame frame = new JFrame("Chat Demo");
        ChatContext ctx = new ChatContext() {
            public NexusClient getClient () {
                return _client;
            }
            public void setMainPanel (JPanel panel) {
                frame.setContentPane(_main = panel);
                _main.revalidate(); // why setContentPane does not automatically trigger a
                                    // revalidation, I cannot venture to guess
            }
            protected NexusClient _client = JVMClient.create(1234);
            protected JPanel _main;
        };

        ctx.setMainPanel(new ConnectPanel(ctx));
        frame.setSize(800, 600);
        frame.setVisible(true);
    }
}
