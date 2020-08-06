package org.slf4j.impl;

import io.sentry.Sentry;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Set;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.event.LoggingEvent;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

public class Logger implements org.slf4j.Logger {
    protected static final SimpleDateFormat simpleDate = new SimpleDateFormat("HH:mm:ss");
    protected static final int LOG_LEVEL_TRACE = 0;
    protected static final int LOG_LEVEL_DEBUG = 10;
    protected static final int LOG_LEVEL_INFO = 20;
    protected static final int LOG_LEVEL_WARN = 30;
    protected static final int LOG_LEVEL_ERROR = 40;
    protected static final int LOG_LEVEL_CONSOLE = 50;

    private static final String ANSI_RESET = "\u001b[0m";
    private static final String ANSI_BLACK = "\u001b[30m";
    private static final String ANSI_RED = "\u001b[31m";
    private static final String ANSI_GREEN = "\u001b[32m";
    private static final String ANSI_YELLOW = "\u001b[33m";
    private static final String ANSI_BLUE = "\u001b[34m";
    private static final String ANSI_PURPLE = "\u001b[35m";
    private static final String ANSI_CYAN = "\u001b[36m";
    private static final String ANSI_WHITE = "\u001b[37m";

    private static final String lineFormat = "[{DATE}] [{LEVEL}-{NAME}] {MESSAGE}";

    private LoggerFactory source;
    private String name;

    private boolean debugLevel = true;
    private boolean traceLevel = true;
    private boolean infoLevel = true;
    private boolean warnLevel = true;
    private boolean errorLevel = true;

    public Logger(String name, LoggerFactory source) {
        this.source = source;
        this.name = name;
        if (!name.equals("ROOT")) {
            Logger main = StaticLoggerBinder.getSingleton().getLoggerFactory().getLogger("ROOT");
            this.traceLevel = main.isTraceEnabled();
            this.debugLevel = main.isDebugEnabled();
            this.infoLevel = main.isInfoEnabled();
            this.warnLevel = main.isWarnEnabled();
            this.errorLevel = main.isErrorEnabled();
        }

    }

    public String getName() {
        return this.name;
    }

    public void setTraceLevel(boolean traceLevel) {
        this.traceLevel = traceLevel;
    }

    public void setDebugLevel(boolean debugLevel) {
        this.debugLevel = debugLevel;
    }

    public void setInfoLevel(boolean infoLevel) {
        this.infoLevel = infoLevel;
    }

    public void setWarnLevel(boolean warnLevel) {
        this.warnLevel = warnLevel;
    }

    public void setErrorLevel(boolean errorLevel) {
        this.errorLevel = errorLevel;
    }

    public void resetLevel() {
        setTraceLevel(false);
        setDebugLevel(false);
        setInfoLevel(false);
        setWarnLevel(false);
        setErrorLevel(false);
    }

    public void setLevel(Level level) {
        resetLevel();

        switch (level) {
            case TRACE: setTraceLevel(true);
            case DEBUG: setDebugLevel(true);
            case INFO:  setInfoLevel(true);
            case WARN:  setWarnLevel(true);
            case ERROR: setErrorLevel(true);
        }
    }

    private void log(int level, String message, Throwable t) {
        if (this.isLevelEnabled(level)) {
            String date = simpleDate.format(new Date());
            String strLevel = this.renderLevel(level);
            String line = this.formatLine(date, strLevel, message);
            this.write(date, strLevel, level, line, t);
        }
    }

    private String formatLine(String date, String level, String message) {
        return "[{DATE}] [{LEVEL}-{NAME}] {MESSAGE}".replace("{DATE}", date).replace("{LEVEL}", level).replace("{NAME}", this.computeShortName()).replace("{MESSAGE}", String.valueOf(message));
    }

    protected String renderLevel(int level) {
        switch(level) {
            case LOG_LEVEL_TRACE:
                return "TRACE";
            case LOG_LEVEL_DEBUG:
                return ("DEBUG");
            case LOG_LEVEL_INFO:
                return "INFO";
            case LOG_LEVEL_WARN:
                return "WARN";
            case LOG_LEVEL_ERROR:
                return "ERROR";
            case LOG_LEVEL_CONSOLE:
                return "CONSOLE";
            default:
                throw new IllegalStateException("Unrecognized level [" + level + "]");
        }
    }

    void write(String date, String strLevel, int level, String buf, Throwable t) {
        PrintStream targetStream = System.out;
        String color = this.getColorByLevel(level);
        targetStream.println(color + buf + Logger.ANSI_RESET);
        this.source.log(buf);
        this.writeThrowable(date, strLevel, color, t, targetStream);
        targetStream.flush();
    }

    protected void writeThrowable(String date, String strLevel, String color, Throwable t, PrintStream targetStream) {
        if (t != null) {
            Sentry.capture(t);

            Set<Throwable> set = Collections.newSetFromMap(new IdentityHashMap<>());
            set.add(t);

            StringBuilder builder = new StringBuilder();
            builder.append(formatLine(date, strLevel, t.toString()));

            StackTraceElement[] trace = t.getStackTrace();
            for (StackTraceElement traceElement : trace) {
                builder.append("\n").append(formatLine(date, strLevel, "\tat " + traceElement));
            }

            for (Throwable se : t.getSuppressed()) {
                builder.append("\n");
                this.printEnclosedStackTrace(se, trace, "Suppressed: ", "\t", set, builder, date, strLevel);
            }

            Throwable ourCause = t.getCause();
            if (ourCause != null) {
                builder.append("\n");
                this.printEnclosedStackTrace(ourCause, trace, "Caused by: ", "", set, builder, date, strLevel);
            }

            String msg = builder.toString();
            targetStream.println(color + msg + ANSI_RESET);
            source.log(msg);
        }
    }

    private void printEnclosedStackTrace(Throwable t, StackTraceElement[] enclosingTrace, String caption, String prefix, Set<Throwable> set, StringBuilder builder, String date, String strLevel) {
        if (set.contains(t)) {
            builder.append(this.formatLine(date, strLevel, "\t[CIRCULAR REFERENCE:" + t + "]"));
        } else {
            set.add(t);
            StackTraceElement[] trace = t.getStackTrace();
            int m = trace.length - 1;

            for(int n = enclosingTrace.length - 1; m >= 0 && n >= 0 && trace[m].equals(enclosingTrace[n]); --n) {
                --m;
            }

            int framesInCommon = trace.length - 1 - m;
            builder.append(this.formatLine(date, strLevel, prefix + caption + t));

            for(int i = 0; i <= m; ++i) {
                builder.append("\n").append(this.formatLine(date, strLevel, prefix + "\tat " + trace[i]));
            }

            if (framesInCommon != 0) {
                builder.append("\n").append(this.formatLine(date, strLevel, prefix + "\t... " + framesInCommon + " more"));
            }

            Throwable[] var17 = t.getSuppressed();
            int var14 = var17.length;

            for(int var15 = 0; var15 < var14; ++var15) {
                Throwable se = var17[var15];
                builder.append("\n");
                this.printEnclosedStackTrace(se, trace, "Suppressed: ", prefix + "\t", set, builder, date, strLevel);
            }

            Throwable ourCause = t.getCause();
            if (ourCause != null) {
                builder.append("\n");
                this.printEnclosedStackTrace(ourCause, trace, "Caused by: ", prefix, set, builder, date, strLevel);
            }
        }

    }

    private String getColorByLevel(int level) {
        switch(level) {
            case LOG_LEVEL_TRACE:   return Logger.ANSI_CYAN;
            case LOG_LEVEL_DEBUG:   return Logger.ANSI_PURPLE;
            case LOG_LEVEL_INFO:    return Logger.ANSI_BLUE;
            case LOG_LEVEL_WARN:    return Logger.ANSI_YELLOW;
            case LOG_LEVEL_ERROR:   return Logger.ANSI_RED;
            case LOG_LEVEL_CONSOLE: return Logger.ANSI_GREEN;
            default:                return Logger.ANSI_WHITE;
        }
    }

    private String getFormattedDate() {
        return simpleDate.format(new Date());
    }

    private String computeShortName() {
        return this.name.substring(this.name.lastIndexOf(".") + 1);
    }

    private void formatAndLog(int level, String format, Object arg1, Object arg2) {
        if (this.isLevelEnabled(level)) {
            FormattingTuple tp = MessageFormatter.format(format, arg1, arg2);
            this.log(level, tp.getMessage(), tp.getThrowable());
        }
    }

    private void formatAndLog(int level, String format, Object... arguments) {
        if (this.isLevelEnabled(level)) {
            FormattingTuple tp = MessageFormatter.arrayFormat(format, arguments);
            this.log(level, tp.getMessage(), tp.getThrowable());
        }
    }

    protected boolean isLevelEnabled(int logLevel) {
        switch(logLevel) {
            case Logger.LOG_LEVEL_TRACE: return this.traceLevel;
            case Logger.LOG_LEVEL_DEBUG: return this.debugLevel;
            case Logger.LOG_LEVEL_INFO:  return this.infoLevel;
            case Logger.LOG_LEVEL_WARN:  return this.warnLevel;
            case Logger.LOG_LEVEL_ERROR: return this.errorLevel;
            default:                     return false;
        }
    }
    /** Are {@code trace} messages currently enabled? */
    public boolean isTraceEnabled() {
        return isLevelEnabled(LOG_LEVEL_TRACE);
    }

    /**
     * A simple implementation which logs messages of level TRACE according to
     * the format outlined above.
     */
    public void trace(String msg) {
        log(LOG_LEVEL_TRACE, msg, null);
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * TRACE according to the format outlined above.
     */
    public void trace(String format, Object param1) {
        formatAndLog(LOG_LEVEL_TRACE, format, param1, null);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * TRACE according to the format outlined above.
     */
    public void trace(String format, Object param1, Object param2) {
        formatAndLog(LOG_LEVEL_TRACE, format, param1, param2);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * TRACE according to the format outlined above.
     */
    public void trace(String format, Object... argArray) {
        formatAndLog(LOG_LEVEL_TRACE, format, argArray);
    }

    /** Log a message of level TRACE, including an exception. */
    public void trace(String msg, Throwable t) {
        log(LOG_LEVEL_TRACE, msg, t);
    }

    /** Are {@code debug} messages currently enabled? */
    public boolean isDebugEnabled() {
        return isLevelEnabled(LOG_LEVEL_DEBUG);
    }

    /**
     * A simple implementation which logs messages of level DEBUG according to
     * the format outlined above.
     */
    public void debug(String msg) {
        log(LOG_LEVEL_DEBUG, msg, null);
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * DEBUG according to the format outlined above.
     */
    public void debug(String format, Object param1) {
        formatAndLog(LOG_LEVEL_DEBUG, format, param1, null);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * DEBUG according to the format outlined above.
     */
    public void debug(String format, Object param1, Object param2) {
        formatAndLog(LOG_LEVEL_DEBUG, format, param1, param2);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * DEBUG according to the format outlined above.
     */
    public void debug(String format, Object... argArray) {
        formatAndLog(LOG_LEVEL_DEBUG, format, argArray);
    }

    /** Log a message of level DEBUG, including an exception. */
    public void debug(String msg, Throwable t) {
        log(LOG_LEVEL_DEBUG, msg, t);
    }

    /** Are {@code info} messages currently enabled? */
    public boolean isInfoEnabled() {
        return isLevelEnabled(LOG_LEVEL_INFO);
    }

    /**
     * A simple implementation which logs messages of level INFO according to
     * the format outlined above.
     */
    public void info(String msg) {
        log(LOG_LEVEL_INFO, msg, null);
    }

    public void console(String msg)
    {
        log(LOG_LEVEL_CONSOLE, msg, null);
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * INFO according to the format outlined above.
     */
    public void info(String format, Object arg) {
        formatAndLog(LOG_LEVEL_INFO, format, arg, null);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * INFO according to the format outlined above.
     */
    public void info(String format, Object arg1, Object arg2) {
        formatAndLog(LOG_LEVEL_INFO, format, arg1, arg2);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * INFO according to the format outlined above.
     */
    public void info(String format, Object... argArray) {
        formatAndLog(LOG_LEVEL_INFO, format, argArray);
    }

    /** Log a message of level INFO, including an exception. */
    public void info(String msg, Throwable t) {
        log(LOG_LEVEL_INFO, msg, t);
    }

    /** Are {@code warn} messages currently enabled? */
    public boolean isWarnEnabled() {
        return isLevelEnabled(LOG_LEVEL_WARN);
    }

    /**
     * A simple implementation which always logs messages of level WARN
     * according to the format outlined above.
     */
    public void warn(String msg) {
        log(LOG_LEVEL_WARN, msg, null);
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * WARN according to the format outlined above.
     */
    public void warn(String format, Object arg) {
        formatAndLog(LOG_LEVEL_WARN, format, arg, null);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * WARN according to the format outlined above.
     */
    public void warn(String format, Object arg1, Object arg2) {
        formatAndLog(LOG_LEVEL_WARN, format, arg1, arg2);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * WARN according to the format outlined above.
     */
    public void warn(String format, Object... argArray) {
        formatAndLog(LOG_LEVEL_WARN, format, argArray);
    }

    /** Log a message of level WARN, including an exception. */
    public void warn(String msg, Throwable t) {
        log(LOG_LEVEL_WARN, msg, t);
    }

    /** Are {@code error} messages currently enabled? */
    public boolean isErrorEnabled() {
        return isLevelEnabled(LOG_LEVEL_ERROR);
    }

    /**
     * A simple implementation which always logs messages of level ERROR
     * according to the format outlined above.
     */
    public void error(String msg) {
        log(LOG_LEVEL_ERROR, msg, null);
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * ERROR according to the format outlined above.
     */
    public void error(String format, Object arg) {
        formatAndLog(LOG_LEVEL_ERROR, format, arg, null);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * ERROR according to the format outlined above.
     */
    public void error(String format, Object arg1, Object arg2) {
        formatAndLog(LOG_LEVEL_ERROR, format, arg1, arg2);
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * ERROR according to the format outlined above.
     */
    public void error(String format, Object... argArray) {
        formatAndLog(LOG_LEVEL_ERROR, format, argArray);
    }

    /** Log a message of level ERROR, including an exception. */
    public void error(String msg, Throwable t) {
        log(LOG_LEVEL_ERROR, msg != null ? msg : t.getMessage(), msg != null ? t : t.getCause());
    }

    public void log(LoggingEvent event) {
        int levelInt = event.getLevel().toInt();

        if (!isLevelEnabled(levelInt)) {
            return;
        }
        FormattingTuple tp = MessageFormatter.arrayFormat(event.getMessage(), event.getArgumentArray(), event.getThrowable());
        log(levelInt, tp.getMessage(), event.getThrowable());
    }

    public boolean isTraceEnabled(Marker marker) {
        return isTraceEnabled();
    }

    public void trace(Marker marker, String msg) {
        trace(msg);
    }

    public void trace(Marker marker, String format, Object arg) {
        trace(format, arg);
    }

    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        trace(format, arg1, arg2);
    }

    public void trace(Marker marker, String format, Object... arguments) {
        trace(format, arguments);
    }

    public void trace(Marker marker, String msg, Throwable t) {
        trace(msg, t);
    }

    public boolean isDebugEnabled(Marker marker) {
        return isDebugEnabled();
    }

    public void debug(Marker marker, String msg) {
        debug(msg);
    }

    public void debug(Marker marker, String format, Object arg) {
        debug(format, arg);
    }

    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        debug(format, arg1, arg2);
    }

    public void debug(Marker marker, String format, Object... arguments) {
        debug(format, arguments);
    }

    public void debug(Marker marker, String msg, Throwable t) {
        debug(msg, t);
    }

    public boolean isInfoEnabled(Marker marker) {
        return isInfoEnabled();
    }

    public void info(Marker marker, String msg) {
        info(msg);
    }

    public void info(Marker marker, String format, Object arg) {
        info(format, arg);
    }

    public void info(Marker marker, String format, Object arg1, Object arg2) {
        info(format, arg1, arg2);
    }

    public void info(Marker marker, String format, Object... arguments) {
        info(format, arguments);
    }

    public void info(Marker marker, String msg, Throwable t) {
        info(msg, t);
    }

    public boolean isWarnEnabled(Marker marker) {
        return isWarnEnabled();
    }

    public void warn(Marker marker, String msg) {
        warn(msg);
    }

    public void warn(Marker marker, String format, Object arg) {
        warn(format, arg);
    }

    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        warn(format, arg1, arg2);
    }

    public void warn(Marker marker, String format, Object... arguments) {
        warn(format, arguments);
    }

    public void warn(Marker marker, String msg, Throwable t) {
        warn(msg, t);
    }

    public boolean isErrorEnabled(Marker marker) {
        return isErrorEnabled();
    }

    public void error(Marker marker, String msg) {
        error(msg);
    }

    public void error(Marker marker, String format, Object arg) {
        error(format, arg);
    }

    public void error(Marker marker, String format, Object arg1, Object arg2) {
        error(format, arg1, arg2);
    }

    public void error(Marker marker, String format, Object... arguments) {
        error(format, arguments);
    }

    public void error(Marker marker, String msg, Throwable t) {
        error(msg, t);
    }

    public String toString() {
        return this.getClass().getName() + "(" + getName() + ")";
    }
}
