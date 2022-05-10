public class LatencyRecord {

    private final String task;
    private final String phase;
    private final Integer requestsPerThread;
    private final String threadID;
    private final Integer requestID;
    private final long start;
    private final long end;
    private final long duration;

    public LatencyRecord(String task, String phase, Integer requestsPerThread, String threadID, Integer requestID, long start, long end, long duration) {
        this.task = task;
        this.phase = phase;
        this.requestsPerThread = requestsPerThread;
        this.threadID = threadID;
        this.requestID = requestID;
        this.start = start;
        this.end = end;
        this.duration = duration;
    }

    public String getTask() {
        return task;
    }

    public String getPhase() {
        return phase;
    }

    public Integer getRequestsPerThread() {
        return requestsPerThread;
    }

    public String getThreadID() {
        return threadID;
    }

    public Integer getRequestID() {
        return requestID;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public long getDuration() {
        return duration;
    }
}
