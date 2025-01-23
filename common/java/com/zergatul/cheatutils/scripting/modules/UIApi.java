package com.zergatul.cheatutils.scripting.modules;

import com.zergatul.cheatutils.scripting.ApiType;
import com.zergatul.cheatutils.scripting.ApiVisibility;
import com.zergatul.scripting.MethodDescription;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.MutableComponent;

import static com.zergatul.cheatutils.utils.ComponentUtils.constructMessage;

@SuppressWarnings("unused")
public class UIApi {

    private final Minecraft mc = Minecraft.getInstance();

    public boolean isDebugScreenEnabled() {
        return mc.gui.getDebugOverlay().showDebugScreen();
    }

    @ApiVisibility({ ApiType.ACTION, ApiType.LOGGING })
    public void systemMessage(String text) {
        showMessage(constructMessage(text), false);
    }

    @ApiVisibility({ ApiType.ACTION, ApiType.LOGGING })
    public void overlayMessage(String text) {
        showMessage(constructMessage(text), true);
    }

    @ApiVisibility({ ApiType.ACTION, ApiType.LOGGING })
    public void systemMessage(String color, String text) {
        showMessage(constructMessage(color, text), false);
    }

    @ApiVisibility({ ApiType.ACTION, ApiType.LOGGING })
    public void overlayMessage(String color, String text) {
        showMessage(constructMessage(color, text), true);
    }

    @ApiVisibility({ ApiType.ACTION, ApiType.LOGGING })
    public void systemMessage(String color1, String text1, String color2, String text2) {
        showMessage(constructMessage(color1, text1, color2, text2), false);
    }

    @ApiVisibility({ ApiType.ACTION, ApiType.LOGGING })
    public void overlayMessage(String color1, String text1, String color2, String text2) {
        showMessage(constructMessage(color1, text1, color2, text2), true);
    }

    @MethodDescription("""
            Saves screenshot, like pressing F2
            """)
    @ApiVisibility(ApiType.ACTION)
    public void screenshot() {
        Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), message -> mc.execute(() -> mc.gui.getChat().addMessage(message)));
    }

    private void showMessage(MutableComponent message, boolean overlay) {
        mc.getChatListener().handleSystemMessage(message, overlay);
    }
}