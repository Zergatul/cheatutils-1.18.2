package com.zergatul.cheatutils.scripting.types;

import com.zergatul.scripting.type.CustomType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@CustomType(name = "Regex")
public class Regex {

    private static final Regex INVALID = new Regex(Pattern.compile(""), false);

    private final Pattern pattern;
    private final boolean valid;

    private Regex(Pattern pattern) {
        this(pattern, true);
    }

    private Regex(Pattern pattern, boolean valid) {
        this.pattern = pattern;
        this.valid = valid;
    }

    public static Regex compile(String regex) {
        try {
            return new Regex(Pattern.compile(regex));
        } catch (PatternSyntaxException e) {
            return INVALID;
        }
    }

    public static Regex compile(String regex, int flags) {
        try {
            return new Regex(Pattern.compile(regex, flags));
        } catch (PatternSyntaxException e) {
            return INVALID;
        }
    }

    public String[] split(String input) {
        if (!this.valid) {
            return new String[0];
        }
        return pattern.split(input);
    }

    public boolean isMatch(String input) {
        if (!this.valid) {
            return false;
        }
        return pattern.matcher(input).matches();
    }

    public Match match(String input) {
        if (!this.valid) {
            return Match.EMPTY;
        }

        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return new Match(matcher.toMatchResult());
        } else {
            return Match.EMPTY;
        }
    }

    public Match[] matches(String input) {
        if (!this.valid) {
            return new Match[0];
        }

        Matcher matcher = pattern.matcher(input);
        return matcher.results().map(Match::new).toArray(Match[]::new);
    }
}