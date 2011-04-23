//
// $Id$
//
// Nexus Core - a framework for developing distributed applications
// http://github.com/threerings/nexus/blob/master/LICENSE

package com.threerings.nexus.util;

/**
 * Handles logging for the Nexus code.
 */
public class Log
{
    /** A level of indirection that allows us to plug in Java or GWT-based logging. */
    public static abstract class Logger {
        /** Logs an info level message, with the supplied arguments and optional Throwable cause in
         * final position. For example:
         * {@code log.info("Thing happened", "info", data, "also", moredata);} */
        public abstract void info (String message, Object... args);

        /** Logs a warning level message, with the supplied arguments and optional Throwable cause
         * in final position. For example:
         * {@code log.info("Bad thing happened", "info", data, cause);} */
        public abstract void warning (String message, Object... args);

        /** Disables info logging, shows only warnings and above. */
        public abstract void setWarnOnly ();

        protected void format (Object level, String message, Object... args)
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
            log(level, sb.toString(), cause);
        }

        /** Called with a formatted log message and (possibly null) cause. */
        protected abstract void log (Object level, String message, Throwable cause);
    }

    /** Dispatch log messages through this instance. */
    public static Logger log = new JavaLogger("nexus");

    private Log () {} // no constructsky

    protected static class JavaLogger extends Logger
    {
        public JavaLogger (String name) {
            _impl = java.util.logging.Logger.getLogger(name);
        }

        public void info (String message, Object... args) {
            if (_impl.isLoggable(java.util.logging.Level.INFO)) {
                format(java.util.logging.Level.INFO, message, args);
            }
        }

        public void warning (String message, Object... args) {
            if (_impl.isLoggable(java.util.logging.Level.WARNING)) {
                format(java.util.logging.Level.WARNING, message, args);
            }
        }

        public void setWarnOnly () {
            _impl.setLevel(java.util.logging.Level.WARNING);
        }

        protected void log (Object level, String message, Throwable cause) {
            _impl.log((java.util.logging.Level)level, message, cause);
        }

        protected final java.util.logging.Logger _impl;
    }
}
