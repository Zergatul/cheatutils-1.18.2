package com.zergatul.cheatutils.scripting.modules;

import com.zergatul.cheatutils.concurrent.TickEndExecutor;
import com.zergatul.cheatutils.scripting.ApiType;
import com.zergatul.cheatutils.scripting.ApiVisibility;
import com.zergatul.scripting.MethodDescription;

public class DelayedApi {

    @MethodDescription("""
            Runs actions after some amount of ticks passed
            """)
    @ApiVisibility(ApiType.ACTION)
    public void run(int ticks, Runnable action) {
        if (ticks <= 0) {
            return;
        }

        TickEndExecutor.instance.waitTicks(ticks, action);
    }
}