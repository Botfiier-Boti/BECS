package com.botifier.becs.util.debugging;

import java.io.PrintStream;


public class ExecutionTimer implements AutoCloseable {

	public final static String DEFAULT_NAME = "Timer";

	private final long start;
	private final long startNs;
	private final PrintStream output;
	private final String name;
	private long end;
	private long endNs;

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
		startNs = System.nanoTime();
		start = System.currentTimeMillis();
	}

	@Override
	public void close() {
		endNs = System.nanoTime() - startNs;
		end = System.currentTimeMillis() - start;

		output.println(name+" Time taken: "+(endNs / 1000000.0)+"ms");
	}

	public long getEndTimeNs() {
		return endNs;
	}

	public long getEndTime() {
		return end;
	}

	public long getStartTime() {
		return start;
	}

	public long getStartTimeNs() {
		return startNs;
	}

}
