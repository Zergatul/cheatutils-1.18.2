package com.zergatul.cheatutils.scripting.modules;

import com.zergatul.cheatutils.modules.hacks.BedrockBreaker;
import com.zergatul.scripting.MethodDescription;

@SuppressWarnings("unused")
public class BedrockBreakerApi {

    @MethodDescription("""
            Attempts to break bedrock block in your crosshair
            """)
    public void process() {
        BedrockBreaker.instance.process();
    }

    @MethodDescription("""
            Attempts to break nearby bedrock blocks in range, one by one
            """)
    public void processNearby() {
        BedrockBreaker.instance.processNearby();
    }

    @MethodDescription("""
            Returns current status of the module. For debugging purposes
            """)
    public String getStatus() {
        return BedrockBreaker.instance.getStatus();
    }
}