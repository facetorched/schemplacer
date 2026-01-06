package com.facetorched.schemplacer.util;

import java.util.function.Supplier;

public class CommandUtil {
	public static <T> T getOptional(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IllegalArgumentException e) {
            // This exception is thrown by Brigadier when an argument isn't found in the context
            return null;
        }
    }
}
