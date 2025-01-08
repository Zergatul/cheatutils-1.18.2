package com.zergatul.cheatutils.scripting.types;

import com.zergatul.scripting.Getter;
import com.zergatul.scripting.type.CustomType;

import java.util.regex.MatchResult;

@CustomType(name = "MatchGroup")
public class MatchGroup {

    private final MatchResult result;
    private final int index;

    public MatchGroup(MatchResult result, int index) {
        this.result = result;
        this.index = index;
    }

    @Getter(name = "index")
    public int getIndex() {
        return index;
    }

    @Getter(name = "length")
    public int getLength() {
        if (0 <= index && index <= result.groupCount()) {
            return result.end(index) - result.start(index);
        } else {
            return -1;
        }
    }

    @Getter(name = "value")
    public String getValue() {
        if (0 <= index && index <= result.groupCount()) {
            return result.group(index);
        } else {
            return "";
        }
    }

    @Getter(name = "valid")
    public boolean getValid() {
        return 0 <= index && index <= result.groupCount();
    }
}