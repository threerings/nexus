//
// $Id$

package nexus.chat.client;

import java.awt.BorderLayout;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.samskivert.nexus.distrib.Address;
import com.samskivert.nexus.distrib.DCustom;
import com.samskivert.nexus.util.Callback;
import com.samskivert.swing.GroupLayout;

import nexus.chat.distrib.ChatObject;
import nexus.chat.distrib.RoomObject;

/**
 * Displays the main chat interface, once connected.
 */
public class ChatPanel extends JPanel
{
    public ChatPanel (ChatContext ctx, ChatObject chatobj)
    {
        _ctx  = ctx;
        _chatobj = chatobj;
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // add the list of rooms on the left
        JPanel left = GroupLayout.makeVStretchBox(5);
        left.add(new JLabel("Rooms"), GroupLayout.FIXED);
        left.add(_rooms = new JList());
        JButton join = new JButton("Join");
        left.add(join, GroupLayout.FIXED);
        add(left, BorderLayout.WEST);

        JPanel main = GroupLayout.makeVStretchBox(5);
        add(main, BorderLayout.CENTER);

        // add a nickname configuration UI up top
        JPanel nickrow = GroupLayout.makeHBox();
        main.add(nickrow, GroupLayout.FIXED);
        nickrow.add(new JLabel("Nickname:"));
        nickrow.add(_nickname = UIUtil.newTextField(200));
        JButton upnick = new JButton("Update");
        nickrow.add(upnick);

        ActionListener onUpNick = new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                updateNickname(_nickname.getText().trim());
            }
        };
        _nickname.addActionListener(onUpNick);
        upnick.addActionListener(onUpNick);

        // add a label displaying the current room name
        main.add(_roomName = new JLabel("Room: <none>"), GroupLayout.FIXED);

        // add the main chat display
        main.add(_chat = new JTextArea());
        _chat.setEditable(false);

        // finally add a UI for entering a chat message
        JPanel chatrow = GroupLayout.makeHStretchBox(5);
        chatrow.add(_entry = new JTextField());
        _entry.setEnabled(false);
        final JButton send = new JButton("Send");
        chatrow.add(send, GroupLayout.FIXED);
        main.add(chatrow, GroupLayout.FIXED);

        // make a request for the current room list and populate our listbox
        _chatobj.chatSvc.get().getRooms(callback(new Action<List<String>>() {
            public void onSuccess (final List<String> rooms) {
                _rooms.setModel(new AbstractListModel() {
                    public int getSize () {
                        return rooms.size();
                    }
                    public Object getElementAt (int idx) {
                        return rooms.get(idx);
                    }
                });
            }
        }, "Failed to fetch rooms"));

        join.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                String room = (String)_rooms.getSelectedValue();
                if (room != null) {
                    joinRoom(room);
                }
            }
        });

        ActionListener sender = new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                String message = _entry.getText().trim();
                if (message.length() > 0) {
                    _entry.setEnabled(false);
                    send.setEnabled(false);
                    _roomobj.roomSvc.get().sendMessage(message, new Callback<Void>() {
                        public void onSuccess (Void result) {
                            _entry.setText("");
                            _entry.setEnabled(true);
                            send.setEnabled(true);
                            _entry.requestFocusInWindow();
                        }
                        public void onFailure (Throwable cause) {
                            feedback("Chat send failed: " + cause.getMessage());
                            _entry.setEnabled(true);
                            send.setEnabled(true);
                        }
                    });
                }
            }
        };
        send.addActionListener(sender);
        _entry.addActionListener(sender);
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
        Action<RoomObject> onJoin = new Action<RoomObject>() {
            public void onSuccess (RoomObject room) {
                if (_roomobj != null) {
                    _ctx.getClient().unsubscribe(_roomobj);
                }
                _roomobj = room;
                _roomobj.chatEvent.addListener(new DCustom.Listener<RoomObject.ChatEvent>() {
                    public void onEvent (RoomObject.ChatEvent event) {
                        if (event.nickname == null) {
                            _chat.append(event.message + "\n"); // from the server
                        } else {
                            _chat.append("<" + event.nickname + "> " + event.message + "\n");
                        }
                    }
                });
                _roomName.setText("Room: " + room.name);
                feedback("Joined room '" + room.name + "'");
                _entry.setEnabled(true);
            }
        };
        _chatobj.chatSvc.get().joinRoom(
            name, _ctx.getClient().subscriber(
                callback(onJoin, "Failed to join room '" + name + "'")));
    }

    protected void feedback (String message)
    {
        _chat.append(message + "\n");
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

    protected ChatContext _ctx;
    protected ChatObject _chatobj;
    protected RoomObject _roomobj;

    protected JList _rooms;
    protected JTextField _nickname;
    protected JLabel _roomName;
    protected JTextArea _chat;
    protected JTextField _entry;
}
