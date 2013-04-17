//
// Nexus Chat Demo - demonstrates Nexus with some chattery
// http://github.com/threerings/nexus/blob/master/LICENSE

package nexus.chat.distrib;

import com.threerings.nexus.distrib.DAttribute;
import com.threerings.nexus.distrib.DService;
import com.threerings.nexus.distrib.NexusObject;
import com.threerings.nexus.distrib.Singleton;

/**
 * A singleton object that vends the chat service.
 */
public class ChatObject extends NexusObject
    implements Singleton
{
    /** Provides global chat services. */
    public final DService<ChatService> chatSvc;

    public ChatObject (DService.Factory<ChatService> chatSvc) {
        this.chatSvc = chatSvc.createService(this);
    }
}
