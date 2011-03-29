//
// $Id$

package nexus.chat.web;

import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;

import com.samskivert.nexus.distrib.DCustom;
import com.samskivert.nexus.util.Callback;

import nexus.chat.distrib.ChatObject;
import nexus.chat.distrib.RoomObject;

/**
 * Displays the main chat interface, once connected.
 */
public class ChatPanel extends FlowPanel
{
    public ChatPanel (WebContext ctx, ChatObject chatobj)
    {
        _ctx  = ctx;
        _chatobj = chatobj;

        final TextBox newroom = Widgets.newTextBox("", 64, 20);
        final Button create = new Button("Create");
        FluentTable rooms = new FluentTable(0, 5);
        rooms.add().setText("Rooms:").right().setWidget(_rooms = Widgets.newFlowPanel()).
            right().setText("Create room:").right().setWidget(newroom).right().setWidget(create);
        add(rooms);
        
//         setLayout(new BorderLayout(5, 5));
//         setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

//         // add the list of rooms on the right
//         JPanel left = GroupLayout.makeVStretchBox(5);
//         left.add(new JLabel("Rooms"), GroupLayout.FIXED);
//         left.add(_rooms = new JList());
//         JButton join = new JButton("Join");
//         left.add(join, GroupLayout.FIXED);
//         add(left, BorderLayout.EAST);

//         join.addActionListener(new ActionListener() {
//             public void actionPerformed (ActionEvent event) {
//                 String room = (String)_rooms.getSelectedValue();
//                 if (room != null) {
//                     joinRoom(room);
//                 }
//             }
//         });

//         JPanel main = GroupLayout.makeVStretchBox(5);
//         add(main, BorderLayout.CENTER);

//         // add a nickname configuration and room creation UI up top
//         final JTextField nickname = new JTextField();
//         JButton upnick = new JButton("Update");
//         ActionListener onUpNick = new ActionListener() {
//             public void actionPerformed (ActionEvent event) {
//                 updateNickname(nickname.getText().trim());
//             }
//         };
//         nickname.addActionListener(onUpNick);
//         upnick.addActionListener(onUpNick);

//         final JTextField roomname = new JTextField();
//         JButton newroom = new JButton("Create");
//         ActionListener creator = new ActionListener() {
//             public void actionPerformed (ActionEvent event) {
//                 createRoom(roomname.getText().trim());
//                 roomname.setText("");
//             }
//         };
//         roomname.addActionListener(creator);
//         newroom.addActionListener(creator);

//         JPanel toprow = GroupLayout.makeHStretchBox(5);
//         main.add(toprow, GroupLayout.FIXED);
//         toprow.add(new JLabel("Nickname:"), GroupLayout.FIXED);
//         toprow.add(nickname);
//         toprow.add(upnick, GroupLayout.FIXED);
//         toprow.add(new JLabel("Create room:"), GroupLayout.FIXED);
//         toprow.add(roomname);
//         toprow.add(newroom, GroupLayout.FIXED);

//         // add a label displaying the current room name
//         main.add(_curRoom = new JLabel("Room: <none>"), GroupLayout.FIXED);

//         // add the main chat display
//         main.add(_chat = new JTextArea());
//         _chat.setEditable(false);

//         // finally add a UI for entering a chat message
//         JPanel chatrow = GroupLayout.makeHStretchBox(5);
//         chatrow.add(_entry = new JTextField());
//         _entry.setEnabled(false);
//         final JButton send = new JButton("Send");
//         chatrow.add(send, GroupLayout.FIXED);
//         main.add(chatrow, GroupLayout.FIXED);

//         ActionListener sender = new ActionListener() {
//             public void actionPerformed (ActionEvent event) {
//                 String message = _entry.getText().trim();
//                 if (message.length() > 0) {
//                     _entry.setEnabled(false);
//                     send.setEnabled(false);
//                     _roomobj.roomSvc.get().sendMessage(message, new Callback<Void>() {
//                         public void onSuccess (Void result) {
//                             _entry.setText("");
//                             _entry.setEnabled(true);
//                             send.setEnabled(true);
//                             _entry.requestFocusInWindow();
//                         }
//                         public void onFailure (Throwable cause) {
//                             feedback("Chat send failed: " + cause.getMessage());
//                             _entry.setEnabled(true);
//                             send.setEnabled(true);
//                         }
//                     });
//                 }
//             }
//         };
//         send.addActionListener(sender);
//         _entry.addActionListener(sender);

        // make a request for the current room list
        refreshRooms();
    }

    protected void refreshRooms ()
    {
        _chatobj.chatSvc.get().getRooms(callback(new Action<List<String>>() {
            public void onSuccess (final List<String> rooms) {
//                 _rooms.setModel(new AbstractListModel() {
//                     public int getSize () {
//                         return rooms.size();
//                     }
//                     public Object getElementAt (int idx) {
//                         return rooms.get(idx);
//                     }
//                 });
            }
        }, "Failed to fetch rooms"));
    }

    protected void updateNickname (final String nickname)
    {
        if (nickname.length() == 0) {
            feedback("Error: can't use blank nickname");
        } else {
            _chatobj.chatSvc.get().updateNick(nickname, callback(new Action<Void>() {
                public void onSuccess (Void result) {
                    feedback("Nickname updated to '" + nickname + "'.");
                }
            }, "Failed to update nickname"));
        }
    }

    protected void joinRoom (final String name)
    {
        if (_roomobj != null && _roomobj.name.equals(name)) {
            return; // no point in noopin'
        }
        Action<RoomObject> onJoin = new Action<RoomObject>() {
            public void onSuccess (RoomObject room) {
                joinedRoom(room);
            }
        };
        _chatobj.chatSvc.get().joinRoom(
            name, _ctx.getClient().subscriber(
                callback(onJoin, "Failed to join room '" + name + "'")));
    }

    protected void createRoom (final String name)
    {
        Action<RoomObject> onCreate = new Action<RoomObject>() {
            public void onSuccess (RoomObject room) {
                joinedRoom(room);
                refreshRooms();
            }
        };
        _chatobj.chatSvc.get().createRoom(
            name, _ctx.getClient().subscriber(
                callback(onCreate, "Failed to create room '" + name + "'")));
    }

    protected void joinedRoom (RoomObject room)
    {
        if (_roomobj != null) {
            _ctx.getClient().unsubscribe(_roomobj);
        }
        _roomobj = room;
        _roomobj.chatEvent.addListener(new DCustom.Listener<RoomObject.ChatEvent>() {
            public void onEvent (RoomObject.ChatEvent event) {
                if (event.nickname == null) {
                    appendLine(event.message); // from the server
                } else {
                    appendLine("<" + event.nickname + "> " + event.message);
                }
            }
        });
        _curRoom.setText("Room: " + room.name);
        feedback("Joined room '" + room.name + "'");
        _entry.setEnabled(true);
        _entry.setFocus(true);
    }

    protected void feedback (String message)
    {
        appendLine(message);
    }

    protected void appendLine (String line)
    {
        // TODO
    }
    
    protected <T> Callback<T> callback (final Action<T> action, final String errpre)
    {
        return new Callback<T>() {
            public void onSuccess (T result) {
                action.onSuccess(result);
            }
            public void onFailure (Throwable cause) {
                feedback(errpre + ": " + cause.getMessage());
            }
        };
    }

    protected interface Action<T> {
        void onSuccess (T result);
    }

    protected WebContext _ctx;
    protected ChatObject _chatobj;
    protected RoomObject _roomobj;

    protected FlowPanel _rooms;
    protected Label _curRoom;
//     protected TextArea _chat;
    protected TextBox _entry;
}