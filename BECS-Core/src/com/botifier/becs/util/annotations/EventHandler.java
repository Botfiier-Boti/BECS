package com.botifier.becs.util.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.botifier.becs.util.events.Event;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {
	int priority() default 0;
	Class<? extends Event> event();
	String origin() default "";
}
