package com.botifier.becs.util.memory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public final class ExceptionFormatter {
	
	private static final VarHandle DETAIL_MESSAGE_HANDLE;
	
	static {
		try {
			MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(Throwable.class, MethodHandles.lookup());
            DETAIL_MESSAGE_HANDLE = lookup.findVarHandle(Throwable.class, "detailMessage", String.class);
		} catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
	}
	
	/**
	 * Formats the supplied exceptions message
	 * Sets Volatilely
	 * 
	 * also refills the stack trace, which is a synchronized function, so can cause a bit of contention
	 * 
	 * @param <T extends Throwable> Type of throwable to modify
	 * @param exception T The exception/throwable to format
	 * @param template String the template to use
	 * @param params Object... The formatting parameters
	 * @return T The exception mutated with new data
	 */
	public static final <T extends Throwable> T formatMessage(T exception, String template, Object... params) {
		String newMessage = String.format(template, params);
		
		DETAIL_MESSAGE_HANDLE.setVolatile(exception, newMessage);
		
		exception.fillInStackTrace();
		
		return exception;
	}
}
