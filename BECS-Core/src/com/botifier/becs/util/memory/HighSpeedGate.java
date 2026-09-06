package com.botifier.becs.util.memory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * High speed gate class
 * Used in place of locks in order to maximize throughput
 */
public class HighSpeedGate implements AutoCloseable{
	//Never directly accessed
	@SuppressWarnings("unused")
	private volatile int state = 0;
	
	/**
	 * VarHandle for the state variable
	 */
	private static final VarHandle STATE_HANDLE;
	static {
		try {
			STATE_HANDLE = MethodHandles.lookup()
										.in(HighSpeedGate.class)
										.findVarHandle(HighSpeedGate.class, "state", int.class);
		}  catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
	}
	
	/**
	 * Attempts to mark the gate as 'in use' 
	 * 
	 * Uses VarHandle.compareAndSet semantics
	 * @return
	 */
	public final boolean tryUse() {
		return STATE_HANDLE.compareAndSet(this, 0, 1);
	}
	
	/**
	 * Variation of tryUse for ergonomic use of AutoClosable
	 * 
	 * @return HighSpeedGate this if tryUse succeeds
	 */
	public final HighSpeedGate tryUseClosable() {
		return tryUse() ? this : null;
	}
	
	/**
	 * Checks if the gate is currently in use
	 * @return
	 */
	public final boolean isBusy() {
		return ((int) STATE_HANDLE.getAcquire(this)) != 0;
	}
	
	/**
	 * Releases the gate
	 * 
	 * Uses VarHandle.SetRelease semantics
	 */
	public final void release() {
		STATE_HANDLE.setRelease(this, 0);
	}

	@Override
	public void close() {
		release();
	}
}
