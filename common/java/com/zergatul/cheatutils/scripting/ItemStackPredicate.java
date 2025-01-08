package com.zergatul.cheatutils.scripting;

import com.zergatul.cheatutils.scripting.types.ItemStackWrapper;

@FunctionalInterface
public interface ItemStackPredicate {
    boolean test(ItemStackWrapper itemStack);
}