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
         * {@code log.warning("Bad thing happened", "info", data, cause);} */
        public abstract void warning (String message, Object... args);

        /** Disables info logging, shows only warnings and above. */
        public abstract void setWarnOnly ();

        protected void format (Object level, String message, Object... args) {
            StringBuilder sb = new StringBuilder();
            Log.format(sb, message, args);
            Object last = (args.length % 2 == 1) ? args[args.length-1] : null;
            Throwable cause = (last instanceof Throwable) ? (Throwable)last : null;
            log(level, sb.toString(), cause);
        }

        /** Called with a formatted log message and (possibly null) cause. */
        protected abstract void log (Object level, String message, Throwable cause);
    }

    public static class JavaLogger extends Logger {
        public JavaLogger (String name) {
            _impl = java.util.logging.Logger.getLogger(name);
        }

        @Override public void info (String message, Object... args) {
            if (_impl.isLoggable(java.util.logging.Level.INFO)) {
                format(java.util.logging.Level.INFO, message, args);
            }
        }

        @Override public void warning (String message, Object... args) {
            if (_impl.isLoggable(java.util.logging.Level.WARNING)) {
                format(java.util.logging.Level.WARNING, message, args);
            }
        }

        @Override public void setWarnOnly () {
            _impl.setLevel(java.util.logging.Level.WARNING);
        }

        @Override protected void log (Object level, String message, Throwable cause) {
            _impl.log((java.util.logging.Level)level, message, cause);
        }

        protected final java.util.logging.Logger _impl;
    }

    /** Dispatch log messages through this instance. */
    public static Logger log = new JavaLogger("nexus");

    /** Formats the supplied message for logging. */
    public static String format (String message, Object... args) {
        return format(new StringBuilder(), message, args).toString();
    }

    /** Formats the supplied message (into {@code into}) for logging. */
    public static StringBuilder format (StringBuilder into, String message, Object... args) {
        into.append(message);
        if (args.length > 1) {
            into.append(" [");
            for (int ii = 0, ll = args.length/2; ii < ll; ii++) {
                if (ii > 0) into.append(", ");
                into.append(args[2*ii]).append("=").append(args[2*ii+1]);
            }
            into.append("]");
        }
        return into;
    }

    private Log () {} // no constructsky
}
