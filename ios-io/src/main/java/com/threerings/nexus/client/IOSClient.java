//
// Nexus iOS IO - I/O and network services for Nexus built on Monotouch via IKVM
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.client;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Executor;

import cli.MonoTouch.Foundation.NSAction;
import cli.MonoTouch.Foundation.NSObject;
import cli.System.Console;

import com.threerings.nexus.net.Connection;
import com.threerings.nexus.net.IOSConnection;
import com.threerings.nexus.util.Callback;
import com.threerings.nexus.util.Log;

/**
 * Provides a Nexus client based on IOS-based I/O.
 */
public class IOSClient extends NexusClient
{
    /**
     * Creates a Nexus client.
     * @param exec the executor on which to dispatch distributed object events.
     * @param port the port on which to connect to servers.
     */
    public static NexusClient create (Executor exec, int port) {
        return new IOSClient(exec, port);
    }

    /**
     * Creates a Nexus client that will dispatch events on an NSObject's main thread.
     * @param obj the object on which to dispatch distributed object events.
     * @param port the port on which to connect to servers.
     */
    public static NexusClient create (final NSObject obj, int port) {
        return create(new Executor() {
            public void execute (final Runnable command) {
                obj.InvokeOnMainThread(new NSAction(new NSAction.Method() {
                    public void Invoke () {
                        command.run();
                    }
                }));
            }
        }, port);
    }

    protected IOSClient (Executor exec, int port) {
        _exec = exec;
        _port = port;
        Log.log = IOS_LOGGER;
    }

    @Override protected void connect (String host, Callback<Connection> callback) {
        new IOSConnection(host, _port, _exec, callback);
    }

    protected Executor _exec;
    protected int _port;

    protected static final Log.Logger IOS_LOGGER = new Log.Logger() {
        @Override public void info (String message, Object... args) {
            if (!_warnOnly) {
                format("", message, args);
            }
        }
        @Override public void warning (String message, Object... args) {
            format("WARN: ", message, args);
        }
        @Override public void log (Object level, String message, Throwable cause) {
            Console.WriteLine(level + message);
            if (cause != null) {
                StringWriter out = new StringWriter();
                cause.printStackTrace(new PrintWriter(out));
                Console.Write(out.toString());
            }
        }
        @Override public void setWarnOnly () {
            _warnOnly = true;
        }
        protected boolean _warnOnly;
    };
}
