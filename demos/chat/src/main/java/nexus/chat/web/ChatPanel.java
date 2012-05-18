//
// Nexus Chat Demo - demonstrates Nexus with some chattery
// http://github.com/threerings/nexus/blob/master/LICENSE

package nexus.chat.web;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.FluentTable;
import com.threerings.gwt.ui.Widgets;

import react.Slot;

import com.threerings.nexus.util.Callback;

import nexus.chat.distrib.ChatObject;
import nexus.chat.distrib.RoomObject;

import static com.threerings.nexus.util.Log.log;

/**
 * Displays the main chat interface, once connected.
 */
public class ChatPanel extends Composite
{
    public ChatPanel (WebContext ctx, ChatObject chatobj) {
        _ctx  = ctx;
        _chatobj = chatobj;

        initWidget(_binder.createAndBindUi(this));

        _upnick.addClickHandler(new ClickHandler() {
            public void onClick (ClickEvent event) {
                updateNickname(_nickname.getText().trim());
            }
        });

        _crroom.addClickHandler(new ClickHandler() {
            public void onClick (ClickEvent event) {
                String room = _newroom.getText().trim();
                if (room.length() > 0) {
                    createRoom(room);
                    _newroom.setText("");
                }
            }
        });

        // disable the chat entry until we enter a room
        _entry.setEnabled(false);
        _send.setEnabled(false);

        ClickHandler onSend = new ClickHandler() {
            public void onClick (ClickEvent event) {
                String message = _entry.getText().trim();
                if (message.length() > 0) {
                    _entry.setEnabled(false);
                    _send.setEnabled(false);
                    _roomobj.roomSvc.get().sendMessage(message, new Callback<Void>() {
                        public void onSuccess (Void result) {
                            _entry.setText("");
                            _entry.setEnabled(true);
                            _entry.setFocus(true);
                            _send.setEnabled(true);
                        }
                        public void onFailure (Throwable cause) {
                            feedback("Chat send failed: " + cause.getMessage());
                            _entry.setEnabled(true);
                            _send.setEnabled(true);
                        }
                    });
                }
            }
        };
        _send.addClickHandler(onSend);
        EnterClickAdapter.bind(_entry, onSend);

        // make a request for the current room list
        refreshRooms();
    }

    protected void refreshRooms () {
        log.info("Refreshing rooms...");
        _chatobj.chatSvc.get().getRooms(callback(new Action<List<String>>() {
            public void onSuccess (final List<String> rooms) {
                _rooms.clear();
                log.info("Refreshing rooms UI " + rooms);
                for (final String room : rooms) {
                    if (_roomobj != null && _roomobj.name.equals(room)) {
                        _rooms.add(Widgets.newHTML("<b>" + room + "</b>", "inline"));
                    } else {
                        _rooms.add(Widgets.newActionLabel(room, "inline", new ClickHandler() {
                            public void onClick (ClickEvent event) {
                                joinRoom(room);
                            }
                        }));
                    }
                    _rooms.add(Widgets.newHTML("&nbsp;", "inline"));
                }
            }
        }, "Failed to fetch rooms"));
    }

    protected void updateNickname (final String nickname) {
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

    protected void joinRoom (final String name) {
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

    protected void createRoom (final String name) {
        Action<RoomObject> onCreate = new Action<RoomObject>() {
            public void onSuccess (RoomObject room) {
                joinedRoom(room);
            }
        };
        _chatobj.chatSvc.get().createRoom(
            name, _ctx.getClient().subscriber(
                callback(onCreate, "Failed to create room '" + name + "'")));
    }

    protected void joinedRoom (RoomObject room) {
        log.info("Joined room " + room.name);
        if (_roomobj != null) {
            _ctx.getClient().unsubscribe(_roomobj);
        }
        _roomobj = room;
        _roomobj.chatEvent.connect(new Slot<RoomObject.ChatEvent>() {
            public void onEmit (RoomObject.ChatEvent event) {
                if (event.nickname == null) {
                    appendLine(event.message); // from the server
                } else {
                    appendLine("<" + event.nickname + "> " + event.message);
                }
            }
        });
        feedback("Joined room '" + room.name + "'");
        _entry.setEnabled(true);
        _entry.setFocus(true);

        // refresh our rooms list, as that will update the rooms UI to show that we're in this room
        refreshRooms();
    }

    protected void feedback (String message) {
        appendLine(message);
    }

    protected void appendLine (String line) {
        _chat.add(Widgets.newLabel(line));
    }

    protected <T> Callback<T> callback (final Action<T> action, final String errpre) {
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

    protected interface Styles extends CssResource {
    }
    protected @UiField Styles _styles;

    protected WebContext _ctx;
    protected ChatObject _chatobj;
    protected RoomObject _roomobj;

    protected @UiField TextBox _nickname;
    protected @UiField Button _upnick;
    protected @UiField TextBox _newroom;
    protected @UiField Button _crroom;

    protected @UiField FlowPanel _rooms;
    protected @UiField FlowPanel _chat;
    protected @UiField TextBox _entry;
    protected @UiField Button _send;

    protected interface Binder extends UiBinder<Widget, ChatPanel> {}
    protected static final Binder _binder = GWT.create(Binder.class);
}
