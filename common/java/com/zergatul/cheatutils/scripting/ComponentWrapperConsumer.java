package com.zergatul.cheatutils.scripting;

import com.zergatul.cheatutils.scripting.types.ComponentWrapper;

@FunctionalInterface
public interface ComponentWrapperConsumer {
    void accept(ComponentWrapper text);
}
