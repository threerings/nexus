//
// $Id$

package nexus.chat.client;

import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.samskivert.swing.GroupLayout;

import nexus.chat.distrib.ChatObject;

/**
 * Displays the main chat interface, once connected.
 */
public class ChatPanel extends JPanel
{
    public ChatPanel (ChatContext ctx, ChatObject chatobj)
    {
        _ctx  = ctx;
        _chatobj = chatobj;

        // add the list of rooms on the left
        JPanel left = GroupLayout.makeVStretchBox(5);
        left.add(new JLabel("Rooms"));
        left.add(_rooms = new JList(), GroupLayout.STRETCH);
        JButton join = new JButton("Join");
        left.add(join);
        add(left, BorderLayout.WEST);

        JPanel main = GroupLayout.makeVStretchBox(5);
        add(main, BorderLayout.CENTER);

        // add a nickname configuration UI up top
        JPanel nickrow = GroupLayout.makeHBox();
        main.add(nickrow);
        nickrow.add(new JLabel("Nickname:"));
        nickrow.add(_nickname = UIUtil.newTextField(200));
        JButton upnick = new JButton("Update");
        nickrow.add(upnick);

        // add the main chat display
        // TODO: add a label displaying the current room?
        main.add(_chat = new JTextArea());

        // finally add a UI for entering a chat message
        JPanel chatrow = GroupLayout.makeHStretchBox(5);
        chatrow.add(_entry = new JTextField());
        JButton send = new JButton("Send");
        chatrow.add(send, GroupLayout.FIXED);
        main.add(chatrow);

        // TODO: wire up action listeners for our buttons and whatnot
    }

    protected ChatContext _ctx;
    protected ChatObject _chatobj;

    protected JList _rooms;
    protected JTextField _nickname;
    protected JTextArea _chat;
    protected JTextField _entry;
}
