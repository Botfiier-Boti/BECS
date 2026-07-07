package com.botifier.becs.util.records;

public record Tuple<T1, T2>(T1 value1, T2 value2) {
	
	@SuppressWarnings("unchecked")
	public static <T1, T2> Tuple<T1, T2>[] newTable(int cap) {
		return new Tuple[cap];
	}
	
}
