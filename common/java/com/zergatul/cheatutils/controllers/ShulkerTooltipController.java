package com.zergatul.cheatutils.controllers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.common.events.ContainerRenderLabelsEvent;
import com.zergatul.cheatutils.common.events.PreRenderTooltipEvent;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.ShulkerTooltipConfig;
import com.zergatul.cheatutils.utils.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector2ic;

// TODO: optimize
public class ShulkerTooltipController {

    public static ShulkerTooltipController instance = new ShulkerTooltipController();

    private static final ResourceLocation CONTAINER_TEXTURE = ResourceLocation.parse("textures/gui/container/shulker_box.png");
    private static final int ImageWidth = 176;
    private static final int ImageHeight = 166;
    private static final int TranslateZ = 250;

    private final Minecraft mc = Minecraft.getInstance();
    private boolean locked = false;
    private boolean allowTooltip = false;
    private ItemStack lockedStack;
    private Matrix4f lockedPose;
    private int lockedX, lockedY;
    private PreRenderTooltipEvent currentEvent;
    private boolean renderAfter;
    private boolean renderToTheLeft;

    private ShulkerTooltipController() {
        Events.PreRenderTooltip.add(this::onPreRenderTooltip);
        Events.TooltipPositioned.add(this::onTooltipPositioned);
        Events.PostRenderTooltip.add(this::onPostRenderTooltip);
        Events.ContainerRenderLabels.add(this::onContainerRenderLabels);
    }

    private void onPreRenderTooltip(PreRenderTooltipEvent event) {
        currentEvent = event;
        renderAfter = false;

        if (locked && !allowTooltip) {
            event.cancel();
            return;
        }

        if (allowTooltip) {
            allowTooltip = false;
            return;
        }

        ShulkerTooltipConfig config = ConfigStore.instance.getConfig().shulkerTooltipConfig;
        if (!config.enabled) {
            clearLocked();
            return;
        }

        if (!ItemUtils.isShulkerBox(event.getItemStack())) {
            clearLocked();
            return;
        }

        if (config.showOriginal) {
            renderAfter = true;
        } else {
            event.cancel();
            renderShulkerTooltip();
        }
    }

    private void onTooltipPositioned(Vector2ic position) {
        if (renderAfter) {
            // render shulker tooltip on the opposite side from vanilla tooltip
            renderToTheLeft = position.x() > currentEvent.getX();
        }
    }

    private void onPostRenderTooltip() {
        if (renderAfter) {
            renderShulkerTooltip();
        }
    }

    private void onContainerRenderLabels(ContainerRenderLabelsEvent event) {
        if (locked) {
            if (Screen.hasControlDown()) {
                PoseStack poseStack = event.getGuiGraphics().pose();

                poseStack.pushPose();
                poseStack.setIdentity();
                poseStack.mulPose(lockedPose);

                int x = lockedX;
                int y = lockedY;

                PoseStack.Pose pose = poseStack.last();
                renderShulkerInventory(event.getGuiGraphics(), lockedStack, pose.pose(), x, y);

                int mx = event.getMouseX();
                int my = event.getMouseY();
                renderTooltip(event.getGuiGraphics(), x, y, mx, my);

                poseStack.popPose();
            } else {
                clearLocked();
            }
        }
    }

    private void renderShulkerTooltip() {
        PoseStack poseStack = currentEvent.getGraphics().pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, TranslateZ);

        // 12 pixels margin from DefaultTooltipPositioner and 4 pixels are vanilla border
        int x, y;
        if (renderAfter) {
            // if we render together with vanilla tooltip there is no choice to render left or right
            if (renderToTheLeft) {
                x = currentEvent.getX() - ImageWidth - 8;
                y = currentEvent.getY() - 16;
            } else {
                x = currentEvent.getX() + 8;
                y = currentEvent.getY() - 16;
            }
        } else {
            x = currentEvent.getX() - ImageWidth - 8;
            y = currentEvent.getY() - 16;
            if (x < 0) {
                // show to the right
                x = currentEvent.getX() + 8;
            }
        }

        if (Screen.hasControlDown()) {
            locked = true;
            lockedPose = poseStack.last().pose();
            lockedStack = currentEvent.getItemStack();
            lockedX = x;
            lockedY = y;
        }

        renderShulkerInventory(currentEvent.getGraphics(), currentEvent.getItemStack(), poseStack.last().pose(), x, y);

        poseStack.popPose();
    }

    private void renderShulkerInventory(GuiGraphics graphics, ItemStack itemStack, Matrix4f matrix, int x, int y) {
        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, CONTAINER_TEXTURE);

        drawTexture(matrix, x, y, ImageWidth, 6, 100, 0, 0, ImageWidth, 6, 256, 256);
        drawTexture(matrix, x, y + 6, ImageWidth, 60, 100, 0, 14, ImageWidth, 60, 256, 256);
        drawTexture(matrix, x, y + 66, ImageWidth, 6, 100, 0, 160, ImageWidth, 6, 256, 256);

        NonNullList<ItemStack> content = ItemUtils.getShulkerContent(itemStack);
        for (int i = 0; i < content.size(); i++) {
            ItemStack slot = content.get(i);
            int slotX = i % 9;
            int slotY = i / 9;
            if (!slot.isEmpty()) {
                renderSlot(graphics, slot, x + 8 + 18 * slotX, y + 10 + 18 * slotY);
            }
        }
    }

    private void renderTooltip(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        NonNullList<ItemStack> content = ItemUtils.getShulkerContent(lockedStack);
        for (int i = 0; i < content.size(); i++) {
            ItemStack slot = content.get(i);
            int slotX = i % 9;
            int slotY = i / 9;
            if (!slot.isEmpty()) {
                if (x + 8 + 18 * slotX <= mouseX && mouseX < x + 8 + 18 * slotX + 16) {
                    if (y + 10 + 18 * slotY <= mouseY && mouseY < y + 10 + 18 * slotY + 16) {
                        allowTooltip = true;
                        graphics.renderTooltip(Minecraft.getInstance().font, slot, mouseX, mouseY);
                    }
                }
            }
        }
    }

    private void drawTexture(Matrix4f matrix, int x, int y, int width, int height, int z, int texX, int texY, int texWidth, int texHeight, int texSizeX, int texSizeY) {
        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        RenderSystem.enableDepthTest();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.addVertex(matrix, x, y, z).setUv(1F * texX / texSizeX, 1F * texY / texSizeY);
        bufferbuilder.addVertex(matrix, x, y + height, z).setUv(1F * texX / texSizeX, 1F * (texY + texHeight) / texSizeY);
        bufferbuilder.addVertex(matrix, x + width, y + height, z).setUv(1F * (texX + texWidth) / texSizeX, 1F * (texY + texHeight) / texSizeY);
        bufferbuilder.addVertex(matrix, x + width, y, z).setUv(1F * (texX + texWidth) / texSizeX, 1F * texY / texSizeY);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
    }

    private void renderSlot(GuiGraphics graphics, ItemStack itemStack, int x, int y) {
        RenderSystem.enableDepthTest();
        graphics.renderFakeItem(itemStack, x, y);
        graphics.renderItemDecorations(mc.font, itemStack, x, y);
    }

    private void clearLocked() {
        locked = false;
        lockedPose = null;
        lockedStack = null;
    }
}