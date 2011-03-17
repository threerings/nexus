//
// $Id$

package nexus.chat.distrib;

import java.util.List;

import com.samskivert.nexus.distrib.NexusService;
import com.samskivert.nexus.util.Callback;

/**
 * Defines global chat services.
 */
public interface ChatService extends NexusService
{
    /**
     * Requests that our nickname be updated.
     */
    void updateNick (String nickname, Callback<Void> callback);

    /**
     * Returns a list of names of active chat rooms.
     */
    void getRooms (Callback<List<String>> callback);

    /**
     * Requests to join the chat room with the specified name. Returns the room object on success
     * (to which the client will be subscribed). Failure communicated via exception message (room
     * no longer exists, access denied).
     */
    void joinRoom (String name, Callback<RoomObject> callback);

    /**
     * Requests that a chat room with the specified name be created. The caller implicitly joins
     * the room (and leaves any room they currently occupy). Returns the newly created room object
     * on success (to which the client will be subscribed).
     */
    void createRoom (String name, Callback<RoomObject> callback);
}