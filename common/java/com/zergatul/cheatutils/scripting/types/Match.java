package com.zergatul.cheatutils.scripting.types;

import com.zergatul.scripting.Getter;
import com.zergatul.scripting.Lazy;
import com.zergatul.scripting.type.CustomType;

import java.util.regex.MatchResult;

@CustomType(name = "Match")
public class Match {

    public static final Match EMPTY = new Match(new EmptyMatchResult());

    private final MatchResult result;
    private final Lazy<MatchGroups> groups;

    public Match(MatchResult result) {
        this.result = result;
        this.groups = new Lazy<>(() -> new MatchGroups(result));
    }

    @Getter(name = "index")
    public int getIndex() {
        return result.start();
    }

    @Getter(name = "length")
    public int getLength() {
        return result.end() - result.start();
    }

    @Getter(name = "value")
    public String getValue() {
        return result.group();
    }

    @Getter(name = "success")
    public boolean getSuccess() {
        return result.hasMatch();
    }

    @Getter(name = "groups")
    public MatchGroups getGroups() {
        return groups.value();
    }

    private static class EmptyMatchResult implements MatchResult {

        @Override
        public int start() {
            return -1;
        }

        @Override
        public int start(int group) {
            return -1;
        }

        @Override
        public int end() {
            return -1;
        }

        @Override
        public int end(int group) {
            return -1;
        }

        @Override
        public String group() {
            return "";
        }

        @Override
        public String group(int group) {
            return "";
        }

        @Override
        public int groupCount() {
            return 0;
        }

        @Override
        public boolean hasMatch() {
            return false;
        }
    }
}