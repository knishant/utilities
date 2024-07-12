package org.nkumar.utilities.traceall;

import org.aspectj.lang.JoinPoint;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple class holding the state of the thread, especially the call stack.
 * It also holds the id of the thread which is immutable.
 * Convenience methods are also provided to directly deal with the stack.
 */
public final class ThreadState {
    //Time when tracing started
    private static final long START_TIME = System.currentTimeMillis();

    //least number of ms a method should take before it is logged.
    //default to 2 ms
    private static final int LEAST_COUNT = Integer.getInteger("org.nkumar.utilities.traceall.leastCount", 2);

    private static int globalThreadId = -1;

    private static synchronized int getNextId() {
        globalThreadId++;
        return globalThreadId;
    }

    private final String threadId;
    private final List<CallElement> callList = new ArrayList<>(100);

    public ThreadState() {
        String tid = String.valueOf(getNextId());
        while (tid.length() < 3) {
            tid = "0" + tid;
        }
        threadId = " " + tid + " ";
    }

    public void pushCallElement(final JoinPoint.StaticPart joinPointStaticPart, final long timestamp) {
        callList.add(new CallElement(joinPointStaticPart, timestamp));
    }

    public void flushCallElement(final JoinPoint.StaticPart joinPointStaticPart, final Writer writer, final boolean success) {
        if (callList.isEmpty()) {
            return;
        }
        final long timestamp = System.currentTimeMillis();
        final int lastIndex = callList.size() - 1;
        final CallElement entryCall = callList.get(lastIndex);
        final long timeDiff = timestamp - entryCall.getTimestamp();
        if (timeDiff > LEAST_COUNT) {
            for (int i = 0; i < callList.size(); i++) {
                final CallElement callElement = callList.get(i);
                if (!callElement.isLogged()) {
                    writeCall(callElement.getSignature(), callElement.getTimestamp() - START_TIME, 0,
                            writer, i, true, true, threadId);
                    callElement.logged();
                }
            }
            writeCall(joinPointStaticPart.toString(), timestamp - START_TIME, timeDiff,
                    writer, lastIndex, false, success, threadId);
        }
        callList.remove(lastIndex);
    }

    private static void writeCall(final String signature, final long timestamp, final long timeDiff,
                                  final Writer writer, final int padding, final boolean in, final boolean success, final String threadId) {
        final StringBuilder builder = new StringBuilder(128);
        Util.pad(timestamp, 8, builder);
        builder.append(' ');
        Util.pad(timeDiff, 6, builder);
        builder.append(threadId);
        builder.append(Util.padding(padding));
        if (in) {
            builder.append("--> ");
        } else {
            if (success) {
                builder.append("<-- ");
            } else {
                builder.append("<xx ");
            }
        }
        builder.append(signature).append('\n');
        try {
            writer.write(builder.toString());
        } catch (IOException ignore) {
            //ignore
        }
    }

    private static final class CallElement {
        private final JoinPoint.StaticPart joinPointStaticPart;
        private final long timestamp;
        private boolean logged;

        CallElement(final JoinPoint.StaticPart joinPointStaticPart, final long timestamp) {
            this.joinPointStaticPart = joinPointStaticPart;
            this.timestamp = timestamp;
        }

        String getSignature() {
            return joinPointStaticPart.toString();
        }

        long getTimestamp() {
            return timestamp;
        }

        boolean isLogged() {
            return logged;
        }

        void logged() {
            this.logged = true;
        }
    }
}
