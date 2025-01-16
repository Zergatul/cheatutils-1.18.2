package com.zergatul.cheatutils.scripting.types;

import com.zergatul.scripting.Getter;
import com.zergatul.scripting.MethodDescription;
import com.zergatul.scripting.type.CustomType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringUtil;

@CustomType(name = "TextStyle")
public class StyleWrapper {

    private final Style inner;

    public StyleWrapper(Style style) {
        this.inner = style;
    }

    @Getter(name = "isBold")
    public boolean isBold() {
        return inner.isBold();
    }

    @Getter(name = "hasClickEvent")
    public boolean hasClickEvent() {
        return inner.getClickEvent() != null;
    }

    @Getter(name = "clickEventType")
    public String getClickEventType() {
        ClickEvent event = inner.getClickEvent();
        if (event == null) {
            return "";
        }
        return event.getAction().toString();
    }

    @MethodDescription("""
            If click event type is RUN_COMMAND, sends this command to server.
            Other click event types are ignored.
            Returns true on success.
            """)
    public boolean click() {
        ClickEvent event = inner.getClickEvent();
        if (event == null) {
            return false;
        }

        if (event.getAction() == ClickEvent.Action.RUN_COMMAND) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return false;
            }
            String command = StringUtil.filterText(event.getValue());
            if (command.startsWith("/")) {
                return mc.player.connection.sendUnsignedCommand(command.substring(1));
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}