//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/samskivert/nexus/blob/master/LICENSE

package com.samskivert.nexus.util;

/**
 * Handles logging for the Nexus code.
 */
public class Log
{
    /** A level of indirection that allows us to plug in Java or GWT-based logging. */
    public interface Logger {
        /** Logs an info level message, with the supplied arguments and optional Throwable cause in
         * final position. For example:
         * {@code log.info("Thing happened", "info", data, "also", moredata);} */
        void info (String message, Object... args);

        /** Logs a warning level message, with the supplied arguments and optional Throwable cause
         * in final position. For example:
         * {@code log.info("Bad thing happened", "info", data, cause);} */
        void warning (String message, Object... args);

        /** Implementation detail that's easier to expose here; please ignore. */
        void log (Object level, String message, Throwable cause);

        /** Disables info logging, shows only warnings and above. */
        void setWarnOnly ();
    }

    // TODO: some secret static initialization that wires up either a Java logger or GWT depending
    // on where we're running

    /** Dispatch log messages through this instance. */
    public static final Logger log = new JavaLogger("nexus");

    private Log () {} // no constructsky

    protected static void format (Logger logger, Object level, String message, Object... args)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        if (args.length > 1) {
            sb.append(" [");
            for (int ii = 0, ll = args.length/2; ii < ll; ii++) {
                if (ii > 0) {
                    sb.append(", ");
                }
                sb.append(args[2*ii]).append("=").append(args[2*ii+1]);
            }
            sb.append("]");
        }
        Object last = (args.length % 2 == 1) ? args[args.length-1] : null;
        Throwable cause = (last instanceof Throwable) ? (Throwable)last : null;
        logger.log(level, sb.toString(), cause);
    }

    protected static class JavaLogger implements Logger
    {
        public JavaLogger (String name) {
            _impl = java.util.logging.Logger.getLogger(name);
        }

        public void info (String message, Object... args) {
            if (_impl.isLoggable(java.util.logging.Level.INFO)) {
                format(this, java.util.logging.Level.INFO, message, args);
            }
        }

        public void warning (String message, Object... args) {
            if (_impl.isLoggable(java.util.logging.Level.WARNING)) {
                format(this, java.util.logging.Level.WARNING, message, args);
            }
        }

        public void log (Object level, String message, Throwable cause) {
            _impl.log((java.util.logging.Level)level, message, cause);
        }

        public void setWarnOnly () {
            _impl.setLevel(java.util.logging.Level.WARNING);
        }

        protected final java.util.logging.Logger _impl;
    }
}
