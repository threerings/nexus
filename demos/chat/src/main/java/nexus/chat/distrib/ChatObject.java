//
// $Id$

package nexus.chat.distrib;

import com.samskivert.nexus.distrib.NexusObject;

/**
 * A singleton object that vends the chat service.
 */
public class ChatObject extends NexusObject
{
    /** Provides global chat services. */
    public ChatService chatService;
}
