//
// Nexus Chat Demo - demonstrates Nexus with some chattery
// http://github.com/threerings/nexus/blob/master/LICENSE

package nexus.chat.client;

import javax.swing.JPanel;

import com.threerings.nexus.client.NexusClient;

/**
 * Provides services to the chat app.
 */
public interface ChatContext
{
    /**
     * Returns a reference to our Nexus client.
     */
    NexusClient getClient ();

    /**
     * Makes the supplied panel the main display.
     */
    void setMainPanel (JPanel panel);
}
