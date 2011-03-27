//
// $Id$

package nexus.chat.web;

import com.samskivert.nexus.io.AbstractSerializer;

import nexus.chat.distrib.ChatObject;
import nexus.chat.distrib.ChatService;
import nexus.chat.distrib.Factory_ChatService;
import nexus.chat.distrib.Factory_RoomService;
import nexus.chat.distrib.RoomObject;
import nexus.chat.distrib.RoomService;
import nexus.chat.distrib.Streamer_ChatObject;
import nexus.chat.distrib.Streamer_RoomObject;

/**
 * Contains the mapping for all streamable classes used by the chat app. (Will some day be
 * auto-generated.)
 */
public class ChatSerializer extends AbstractSerializer
{
    public ChatSerializer ()
    {
        mapStreamer(new Streamer_ChatObject(), ChatObject.class);
        mapStreamer(new Streamer_RoomObject(), RoomObject.class);
        mapStreamer(new Streamer_RoomObject.ChatEvent(), RoomObject.ChatEvent.class);
        mapService(new Factory_ChatService(), ChatService.class);
        mapService(new Factory_RoomService(), RoomService.class);
    }
}
