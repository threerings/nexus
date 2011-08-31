//
// Nexus Chat Demo - demonstrates Nexus with some chattery
// http://github.com/threerings/nexus/blob/master/LICENSE

package nexus.chat.web;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.nexus.client.NexusClient;

/**
 * Provides services to the chat webapp.
 */
public interface WebContext
{
    /**
     * Returns a reference to our Nexus client.
     */
    NexusClient getClient ();

    /**
     * Makes the supplied widget the main display.
     */
    void setMainPanel (Widget panel);
}
