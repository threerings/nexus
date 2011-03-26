//
// $Id$

package nexus.chat.distrib;

import com.samskivert.nexus.io.Streamable;
import com.samskivert.nexus.io.Streamer;

/**
 * Streams {@link ChatObject}.
 */
public class Streamer_ChatObject implements Streamer<ChatObject>
{
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
