//
// $Id$

package nexus.chat.distrib;

import com.samskivert.nexus.distrib.DAttribute;
import com.samskivert.nexus.distrib.DService;
import com.samskivert.nexus.distrib.NexusObject;
import com.samskivert.nexus.distrib.Singleton;

/**
 * A singleton object that vends the chat service.
 */
public class ChatObject extends NexusObject
    implements Singleton
{
    /** Provides global chat services. */
    public DService<ChatService> chatSvc;

    public ChatObject (DService<ChatService> chatSvc)
    {
        this.chatSvc = chatSvc;
    }

    @Override
    protected DAttribute getAttribute (int index)
    {
        switch (index) {
        case 0: return chatSvc;
        default: throw new IndexOutOfBoundsException("Invalid attribute index " + index);
        }
    }

    @Override
    protected int getAttributeCount ()
    {
        return 1;
    }
}
