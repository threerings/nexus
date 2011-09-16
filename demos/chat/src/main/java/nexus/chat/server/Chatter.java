//
// Nexus Chat Demo - demonstrates Nexus with some chattery
// http://github.com/threerings/nexus/blob/master/LICENSE

package nexus.chat.server;

import com.threerings.nexus.distrib.Action;
import com.threerings.nexus.distrib.Nexus;

import nexus.chat.client.ChatPanel;

/**
 * A class used to store session-local data for a chatter.
 */
public class Chatter
{
    /** This chatter's configured nickname. */
    public String nickname;

    public Chatter (Nexus nexus, String nickname) {
        this.nickname = nickname;
        _nexus = nexus;
    }

    public void enterRoom (String name) {
        leaveRoom(); // leave any current room
        _name = name;
        _nexus.invoke(RoomManager.class, _name, new Action<RoomManager>() {
            public void invoke (RoomManager mgr) {
                mgr.chatterEntered(nickname);
            }
        });
    }

    public void leaveRoom () {
        if (_name != null) {
            _nexus.invoke(RoomManager.class, _name, new Action<RoomManager>() {
                public void invoke (RoomManager mgr) {
                    mgr.chatterLeft(nickname);
                }
            });
            _name = null;
        }
    }

    public void updateNick (final String nickname) {
        if (_name != null) {
            final String onickname = this.nickname;
            _nexus.invoke(RoomManager.class, _name, new Action<RoomManager>() {
                public void invoke (RoomManager mgr) {
                    mgr.chatterChangedNick(onickname, nickname);
                }
            });
        }
        this.nickname = nickname;
    }

    /** Used to communicate with our occupied room. */
    protected Nexus _nexus;

    /** The name of the current room occupied by this chatter. */
    protected String _name;
}
