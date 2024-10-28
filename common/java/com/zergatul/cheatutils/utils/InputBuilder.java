package com.zergatul.cheatutils.utils;

import net.minecraft.world.entity.player.Input;

public class InputBuilder {

    private boolean forward;
    private boolean backward;
    private boolean left;
    private boolean right;
    private boolean jump;
    private boolean shift;
    private boolean sprint;

    public InputBuilder(Input from) {
        forward = from.forward();
        backward = from.backward();
        left = from.left();
        right = from.right();
        jump = from.jump();
        shift = from.shift();
        sprint = from.sprint();
    }

    public InputBuilder shift(boolean value) {
        shift = value;
        return this;
    }

    public Input build() {
        return new Input(forward, backward, left, right, jump, shift, sprint);
    }
}