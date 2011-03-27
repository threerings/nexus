//
// $Id$

package nexus.chat.web;

import com.google.gwt.user.client.ui.Panel;

import com.samskivert.nexus.client.NexusClient;

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
     * Makes the supplied panel the main display.
     */
    void setMainPanel (Panel panel);
}
