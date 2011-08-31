//
// Nexus Chat Demo - demonstrates Nexus with some chattery
// http://github.com/threerings/nexus/blob/master/LICENSE

package nexus.chat.distrib;

import com.threerings.nexus.io.Streamable;
import com.threerings.nexus.io.Streamer;

/**
 * Handles the streaming of {@link ChatObject} and/or nested classes.
 */
public class Streamer_ChatObject
    implements Streamer<ChatObject>
{
    @Override
    public Class<?> getObjectClass () {
        return ChatObject.class;
    }

    @Override
    public void writeObject (Streamable.Output out, ChatObject obj) {
        writeObjectImpl(out, obj);
        obj.writeContents(out);
    }

    @Override
    public ChatObject readObject (Streamable.Input in) {
        ChatObject obj = new ChatObject(
            in.<ChatService>readService()
        );
        obj.readContents(in);
        return obj;
    }

    public static  void writeObjectImpl (Streamable.Output out, ChatObject obj) {
        out.writeService(obj.chatSvc);
    }
}
