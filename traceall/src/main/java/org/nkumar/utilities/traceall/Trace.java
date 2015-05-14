package org.nkumar.utilities.traceall;

import org.aspectj.lang.JoinPoint;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

public final class Trace
{
    private static final ThreadLocal<ThreadState> threadStateHolder = new ThreadLocal<ThreadState>()
    {
        @Override
        protected synchronized ThreadState initialValue()
        {
            return new ThreadState();
        }

        @Override
        public void set(final ThreadState value)
        {
            throw new UnsupportedOperationException("thread state reference is immutable");
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException("thread state reference is immutable");
        }
    };

    private static final TrackerWriter out;

    private Trace()
    {
    }

    static
    {
        try
        {
            final String tempPath = System.getProperty("java.io.tmpdir");
            final File tempFile = new File(tempPath, "traceall.log");
            System.out.println("tempFile.getAbsolutePath() = " + tempFile.getAbsolutePath());
            out = new TrackerWriter(
                    new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(tempFile)), "US-ASCII"));
            final Timer timer = new Timer("TracerTimer");
            timer.scheduleAtFixedRate(new FlushTask(out), 0, 2000);
        }
        catch (UnsupportedEncodingException ignore)
        {
            throw new AssertionError("US-ASCII must be supported by all the jvms");
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException("unable to create a  log file", e);
        }
    }

    public static void traceEntry(final JoinPoint.StaticPart joinPointStaticPart)
    {
        final ThreadState threadState = threadStateHolder.get();
        threadState.pushCallElement(joinPointStaticPart, System.currentTimeMillis());
    }

    public static void traceExit(final JoinPoint.StaticPart joinPointStaticPart, final boolean success)
    {
        final ThreadState threadState = threadStateHolder.get();
        threadState.flushCallElement(joinPointStaticPart, out, success);
    }

    private static final class FlushTask extends TimerTask
    {
        private final TrackerWriter out;

        private long lastFlushTime;

        private FlushTask(TrackerWriter out)
        {
            this.out = out;
        }

        @Override
        public void run()
        {
            try
            {
                final long lastWriteTimestamp = out.getLastWriteTimestamp();
                final long now = System.currentTimeMillis();
                //if last write was before 5 seconds
                //and there was some write since the last flush
                if ((now - lastWriteTimestamp) > 5000 && lastWriteTimestamp > lastFlushTime)
                {
                    out.write("##################################################"
                            + "##################################################\n");
                    out.flush();
                    lastFlushTime = now;
                }
            }
            catch (Exception ignore)
            {
            }
        }
    }

    //tracks the timestamp when write(String) was called
    private static final class TrackerWriter extends Writer
    {
        private final Writer out;
        private final AtomicLong timestamp = new AtomicLong(0);

        private TrackerWriter(Writer out)
        {
            this.out = out;
        }

        @Override
        public void write(String str) throws IOException
        {
            timestamp.set(System.currentTimeMillis());
            out.write(str);
        }

        @Override
        public void write(char cbuf[], int off, int len) throws IOException
        {
            out.write(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException
        {
            out.flush();
        }

        @Override
        public void close() throws IOException
        {
            out.close();
        }

        public long getLastWriteTimestamp()
        {
            return timestamp.longValue();
        }
    }
}
