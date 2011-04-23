//
// $Id$

package nexus.chat.server;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.samskivert.util.OneLineLogFormatter;

import com.threerings.nexus.server.GWTConnectionManager;
import com.threerings.nexus.server.JVMConnectionManager;
import com.threerings.nexus.server.NexusConfig;
import com.threerings.nexus.server.NexusServer;

import nexus.chat.web.ChatSerializer;

/**
 * Operates the chat server.
 */
public class ChatServer
{
    public static void main (String[] args)
        throws IOException
    {
        Properties props = new Properties();
        props.setProperty("nexus.node", "test");
        props.setProperty("nexus.hostname", "localhost");
        props.setProperty("nexus.rpc_timeout", "1000");
        NexusConfig config = new NexusConfig(props);

        // improve our logging output
        OneLineLogFormatter.configureDefaultHandler(false);

        // create our server
        ExecutorService exec = Executors.newFixedThreadPool(3);
        NexusServer server = new NexusServer(config, exec);

        // create our singleton chat manager
        new ChatManager(server);

        // set up a connection manager and listen on a port
        final JVMConnectionManager jvmmgr = new JVMConnectionManager(server.getSessionManager());
        jvmmgr.listen(config.publicHostname, 1234);
        jvmmgr.start();

        // set up a Jetty instance and our GWTIO servlet
        final GWTConnectionManager gwtmgr = new GWTConnectionManager(
            server.getSessionManager(), new ChatSerializer(), config.publicHostname, 6502);
        gwtmgr.setDocRoot(new File("dist/webapp"));
        gwtmgr.start();
    }
}
