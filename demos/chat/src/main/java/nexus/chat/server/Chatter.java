//
// $Id$

package nexus.chat.server;

/**
 * A class used to store session-local data for a chatter.
 */
public class Chatter
{
    /** This chatter's configured nickname. */
    public String nickname;

    public void enterRoom (RoomManager currentRoom)
    {
        leaveRoom(); // leave any current room
        _currentRoom = currentRoom;
        _currentRoom.chatterEntered(nickname);
    }

    public void leaveRoom ()
    {
        if (_currentRoom != null) {
            _currentRoom.chatterLeft(nickname);
            _currentRoom = null;
        }
    }

    public void updateNick (String nickname)
    {
        if (_currentRoom != null) {
            _currentRoom.chatterChangedNick(this.nickname, nickname);
        }
        this.nickname = nickname;
    }

    /** The current room occupied by this chatter. */
    protected RoomManager _currentRoom;
}
