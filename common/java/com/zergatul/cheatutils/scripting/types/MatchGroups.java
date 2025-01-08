package com.zergatul.cheatutils.scripting.types;

import com.zergatul.scripting.Getter;
import com.zergatul.scripting.IndexGetter;
import com.zergatul.scripting.type.CustomType;

import java.util.regex.MatchResult;

@CustomType(name = "MatchGroups")
public class MatchGroups {

    private final MatchResult result;

    public MatchGroups(MatchResult result) {
        this.result = result;
    }

    @Getter(name = "count")
    public int getCount() {
        return result.groupCount();
    }

    @IndexGetter
    public MatchGroup getGroup(int index) {
        if (index < 0 || index > result.groupCount()) {
            return new MatchGroup(result, Integer.MIN_VALUE);
        } else {
            return new MatchGroup(result, index);
        }
    }

    @IndexGetter
    public MatchGroup getGroup(String name) {
        Integer index = result.namedGroups().get(name);
        if (index == null || index < 0 || index > result.groupCount()) {
            return new MatchGroup(result, Integer.MIN_VALUE);
        } else {
            return new MatchGroup(result, index);
        }
    }
}