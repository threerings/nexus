//
// $Id$

package nexus.chat.server;

import com.samskivert.nexus.util.Callback;

import nexus.chat.distrib.RoomService;

/**
 * Manages a single chat room.
 */
public class RoomManager implements RoomService
{
    // from interface XXX
    public void sendMessage (String message, Callback<Void> callback)
    {
    }
}
