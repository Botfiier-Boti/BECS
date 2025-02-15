package com.botifier.becs.util.debugging;

import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;


/**
 * ExecutionTimer
 * 
 * for some testing
 */
public class ExecutionTimer implements AutoCloseable {

	public final static String DEFAULT_NAME = "Timer";

	private final Instant start;
	private final PrintStream output;
	private final String name;
	private Instant end;
	private boolean closed = false;

	public ExecutionTimer() {
		this(DEFAULT_NAME, System.out);
	}

	public ExecutionTimer(String name) {
		this(name, System.out);
	}

	public ExecutionTimer(PrintStream output) {
		this(DEFAULT_NAME, output);
	}

	public ExecutionTimer(String name, PrintStream output) {
		this.name = name;
		this.output = output;
		start = Instant.now();
	}

	@Override
	public void close() {
		if (closed) 
			return;
		closed = true;
		
		this.end = Instant.now();
		Duration dur = Duration.between(start, end);

		output.printf("%s Time taken: %.3f ms (%d ns)%n", name, dur.toMillis(), dur.toNanos());
	}
	
	public long getElapsedTimeMs() {
        return closed ? Duration.between(start, end).toMillis() : -1;
    }

    public long getElapsedTimeNs() {
        return closed ? Duration.between(start, end).toNanos() : -1;
    }

	public Instant getEndTime() {
		return end;
	}

	public Instant getStartTime() {
		return start;
	}

}
