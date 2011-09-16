//
// Nexus Chat Demo - demonstrates Nexus with some chattery
// http://github.com/threerings/nexus/blob/master/LICENSE

package nexus.chat.client;

import java.awt.Dimension;
import javax.swing.JTextField;

/**
 * User interface utility methods.
 */
public class UIUtil
{
    /**
     * Creates a text field that prefers the supplied width.
     */
    public static JTextField newTextField (final int width) {
        // that this is not achievable more simply, simply boggles the mind
        return new JTextField() {
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width = Math.max(width, d.width);
                return d;
            }
        };
    }
}
