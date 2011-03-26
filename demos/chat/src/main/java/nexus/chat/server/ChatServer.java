//
// $Id$

package nexus.chat.server;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.samskivert.util.OneLineLogFormatter;

import com.samskivert.nexus.server.JVMConnectionManager;
import com.samskivert.nexus.server.NexusConfig;
import com.samskivert.nexus.server.NexusServer;

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
        final JVMConnectionManager conmgr = new JVMConnectionManager(server.getSessionManager());
        conmgr.listen(config.publicHostname, 1234);
        conmgr.start();
    }
}
