//
// $Id$

package nexus.chat.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Streams {@link ChatObject}.
 */
public class Streamer_ChatObject implements Streamer<ChatObject>
{
    public Class<?> getObjectClass ()
    {
        return ChatObject.class;
    }

    public void writeObject (Streamable.Output out, ChatObject obj)
    {
        out.writeService(ChatService.class);
        obj.writeContents(out);
    }

    public ChatObject readObject (Streamable.Input in)
    {
        ChatObject obj = new ChatObject(in.<ChatService>readService());
        obj.readContents(in);
        return obj;
    }
}
