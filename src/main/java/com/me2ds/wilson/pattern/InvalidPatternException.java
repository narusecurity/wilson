package com.me2ds.wilson.pattern;

/**
 * Created by w3kim on 15-06-28.
 */
public class InvalidPatternException extends PatternException {

    public InvalidPatternException(String pattern) {
        super("Cannot recognize the pattern: " + pattern);
    }
}
