//
// $Id$

package nexus.chat.distrib;

import com.samskivert.nexus.distrib.NexusObject;
import com.samskivert.nexus.distrib.Singleton;

/**
 * A singleton object that vends the chat service.
 */
public class ChatObject extends NexusObject
    implements Singleton
{
    /** Provides global chat services. */
    public ChatService chatService;
}
