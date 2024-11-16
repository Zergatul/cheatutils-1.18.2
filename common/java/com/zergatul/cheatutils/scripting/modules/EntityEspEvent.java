package com.zergatul.cheatutils.scripting.modules;

import com.zergatul.cheatutils.font.StylizedText;
import com.zergatul.cheatutils.font.StylizedTextChunk;
import com.zergatul.cheatutils.modules.esp.EntityEsp;
import com.zergatul.cheatutils.utils.ColorUtils;
import com.zergatul.scripting.MethodDescription;
import net.minecraft.network.chat.Style;

@SuppressWarnings("unused")
public class EntityEspEvent {

    private final EntityEsp.EntityScriptResult result;

    public EntityEspEvent(EntityEsp.EntityScriptResult result) {
        this.result = result;
    }

    @MethodDescription("""
            Disables tracer for current entity
            """)
    public void disableTracer() {
        result.tracerDisabled = true;
    }

    @MethodDescription("""
            Disables outline for current entity
            """)
    public void disableOutline() {
        result.outlineDisabled = true;
    }

    @MethodDescription("""
            Disables overlay for current entity
            """)
    public void disableOverlay() {
        result.overlayDisabled = true;
    }

    @MethodDescription("""
            Disables collision box for current entity
            """)
    public void disableCollisionBox() {
        result.collisionBoxDisabled = true;
    }

    @MethodDescription("""
            Overrides title displayed above the entity. Works only with cheatutils title system
            """)
    public void setTitle(String title) {
        result.title = StylizedText.of(title);
    }

    @MethodDescription("""
            Overrides title displayed above the entity. Works only with cheatutils title system.
            Array length must be divisible by 2. Example: [color1, text1, color2, text2]
            """)
    public void setTitle(String[] data) {
        if (data.length % 2 != 0) {
            return;
        }

        StylizedText text = new StylizedText();
        for (int i = 0; i < data.length; i += 2) {
            Integer color = ColorUtils.parseColor(data[i]);
            if (color == null) {
                color = 0xFFFFFF;
            }
            text.chunks.add(new StylizedTextChunk(data[i + 1], Style.EMPTY.withColor(color)));
        }

        result.title = text;
    }
}