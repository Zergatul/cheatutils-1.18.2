package com.zergatul.cheatutils.modules.scripting;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.StatusOverlayConfig;
import com.zergatul.cheatutils.modules.Module;
import com.zergatul.cheatutils.render.Primitives;
import com.zergatul.cheatutils.common.events.RenderGuiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.opengl.GL11;

import java.util.*;

public class StatusOverlay implements Module {

    public static final StatusOverlay instance = new StatusOverlay();

    private static final int TranslateZ = 200;
    private static final int DefaultBackground = 0x90505050;

    private static final Minecraft mc = Minecraft.getInstance();
    private final Map<Align, List<AlignedText>> texts = new HashMap<>();
    private final List<FreeText> freeTexts = new ArrayList<>();
    private Runnable script;
    private HorizontalAlign hAlign;
    private VerticalAlign vAlign;

    private StatusOverlay() {
        for (Align align: Align.values()) {
            texts.put(align, new ArrayList<>());
        }

        Events.PostRenderGui.add(this::render);
    }

    public void setScript(Runnable script) {
        this.script = script;
    }

    public void addText(MutableComponent message) {
        addText(DefaultBackground, message);
    }

    public void addText(int background, MutableComponent message) {
        texts.get(Align.get(vAlign, hAlign)).add(new AlignedText(background, message));
    }

    public void addFreeText(int x, int y, MutableComponent message) {
        addFreeText(x, y, DefaultBackground, message);
    }

    public void addFreeText(int x, int y, int background, MutableComponent message) {
        freeTexts.add(new FreeText(x, y, background, message));
    }

    public void setHorizontalAlign(HorizontalAlign align) {
        hAlign = align;
    }

    public void setVerticalAlign(VerticalAlign align) {
        vAlign = align;
    }

    private void render(RenderGuiEvent event) {
        if (mc.player == null) {
            return;
        }

        StatusOverlayConfig config = ConfigStore.instance.getConfig().statusOverlayConfig;
        if (!config.enabled || script == null) {
            return;
        }

        for (Align align: Align.values()) {
            texts.get(align).clear();
        }

        freeTexts.clear();

        hAlign = HorizontalAlign.RIGHT;
        vAlign = VerticalAlign.BOTTOM;
        script.run();

        PoseStack poseStack = event.getGuiGraphics().pose();
        poseStack.pushPose();
        poseStack.setIdentity();
        poseStack.translate(0, 0, TranslateZ);

        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        for (Align align: Align.values()) {
            List<AlignedText> list = texts.get(align);
            if (list.isEmpty()) {
                continue;
            }
            for (int i = 0; i < list.size(); i++) {
                AlignedText text = list.get(i);
                int width = mc.font.width(text.component);
                int x = getLeft(align.hAlign, mc.getWindow().getGuiScaledWidth(), width);
                int y = getTop(align.vAlign, mc.getWindow().getGuiScaledHeight(), mc.font.lineHeight, i, list.size());
                Primitives.fill(poseStack, x, y, x + width, y + mc.font.lineHeight, text.background);
                event.getGuiGraphics().drawString(mc.font, text.component, x, y, 16777215);
            }
        }

        for (FreeText text: freeTexts) {
            int width = mc.font.width(text.component);
            Primitives.fill(poseStack, text.x, text.y, text.x + width, text.y + mc.font.lineHeight, text.background);
            event.getGuiGraphics().drawString(mc.font, text.component, text.x, text.y, 16777215);
        }

        poseStack.popPose();
    }

    private int getLeft(HorizontalAlign align, int screenWidth, int textWidth) {
        return switch (align) {
            case LEFT -> 2;
            case CENTER -> (screenWidth - textWidth) / 2;
            case RIGHT -> screenWidth - 2 - textWidth;
        };
    }

    private int getTop(VerticalAlign align, int screenHeight, int textHeight, int index, int count) {
        return switch (align) {
            case TOP -> 2 + index * textHeight;
            case MIDDLE -> (screenHeight - textHeight * count) / 2 + index * textHeight;
            case BOTTOM -> screenHeight - 2 - textHeight * (count - index);
        };
    }

    public enum HorizontalAlign {
        LEFT,
        CENTER,
        RIGHT
    }

    public enum VerticalAlign {
        TOP,
        MIDDLE,
        BOTTOM
    }

    private enum Align {
        TOP_LEFT(VerticalAlign.TOP, HorizontalAlign.LEFT),
        TOP_CENTER(VerticalAlign.TOP, HorizontalAlign.CENTER),
        TOP_RIGHT(VerticalAlign.TOP, HorizontalAlign.RIGHT),
        MIDDLE_LEFT(VerticalAlign.MIDDLE, HorizontalAlign.LEFT),
        MIDDLE_CENTER(VerticalAlign.MIDDLE, HorizontalAlign.CENTER),
        MIDDLE_RIGHT(VerticalAlign.MIDDLE, HorizontalAlign.RIGHT),
        BOTTOM_LEFT(VerticalAlign.BOTTOM, HorizontalAlign.LEFT),
        BOTTOM_CENTER(VerticalAlign.BOTTOM, HorizontalAlign.CENTER),
        BOTTOM_RIGHT(VerticalAlign.BOTTOM, HorizontalAlign.RIGHT);

        private final VerticalAlign vAlign;
        private final HorizontalAlign hAlign;

        Align(VerticalAlign vAlign, HorizontalAlign hAlign) {
            this.vAlign = vAlign;
            this.hAlign = hAlign;
        }

        public static Align get(VerticalAlign vAlign, HorizontalAlign hAlign) {
            return switch (vAlign) {
                case TOP -> switch (hAlign) {
                    case LEFT -> TOP_LEFT;
                    case CENTER -> TOP_CENTER;
                    case RIGHT -> TOP_RIGHT;
                };
                case MIDDLE -> switch (hAlign) {
                    case LEFT -> MIDDLE_LEFT;
                    case CENTER -> MIDDLE_CENTER;
                    case RIGHT -> MIDDLE_RIGHT;
                };
                case BOTTOM -> switch (hAlign) {
                    case LEFT -> BOTTOM_LEFT;
                    case CENTER -> BOTTOM_CENTER;
                    default -> BOTTOM_RIGHT;
                };
            };
        }
    }

    private record AlignedText(int background, MutableComponent component) {}

    private record FreeText(int x, int y, int background, MutableComponent component) {}
}