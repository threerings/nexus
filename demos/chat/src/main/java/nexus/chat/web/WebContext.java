//
// $Id$

package nexus.chat.web;

import com.google.gwt.user.client.ui.Widget;

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
     * Makes the supplied widget the main display.
     */
    void setMainPanel (Widget panel);
}
