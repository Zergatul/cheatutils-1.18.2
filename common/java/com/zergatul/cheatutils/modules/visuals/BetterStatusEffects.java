package com.zergatul.cheatutils.modules.visuals;

import com.google.common.collect.Ordering;
import com.mojang.blaze3d.systems.RenderSystem;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.mixins.common.accessors.GuiAccessor;
import com.zergatul.cheatutils.modules.Module;
import com.zergatul.cheatutils.render.Primitives;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.List;

public class BetterStatusEffects implements Module {

    public static final BetterStatusEffects instance = new BetterStatusEffects();

    private final Minecraft mc = Minecraft.getInstance();

    private BetterStatusEffects() {

    }

    public void render(GuiGraphics graphics, Player player, int y) {
        if (!ConfigStore.instance.getConfig().statusEffectsConfig.enabled) {
            return;
        }

        Collection<MobEffectInstance> collection = player.getActiveEffects();
        if (collection.isEmpty()) {
            return;
        }

        MobEffectTextureManager manager = mc.getMobEffectTextures();

        int left = (graphics.guiWidth() - collection.size() * 25) / 2;

        List<MobEffectInstance> sorted = Ordering.natural().sortedCopy(collection);
        for (int i = 0; i < sorted.size(); i++) {
            MobEffectInstance effectInstance = sorted.get(i);

            if (effectInstance.isAmbient()) {
                graphics.blitSprite(RenderType::guiTextured, GuiAccessor.getEffectBackgroundAmbientSprite(), left, y, 24, 24);
            } else {
                graphics.blitSprite(RenderType::guiTextured, GuiAccessor.getEffectBackgroundSprite(), left, y, 24, 24);
            }

            TextureAtlasSprite textureatlassprite = manager.get(effectInstance.getEffect());
            graphics.blitSprite(RenderType::guiTextured, textureatlassprite, left + 3, y + 3, 18, 18);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            String duration = MobEffectUtil.formatDuration(effectInstance, 1, mc.level.tickRateManager().tickrate()).getString();
            if (duration.startsWith("00")) {
                duration = duration.substring(1);
            }
            if (!duration.startsWith("0:") && duration.startsWith("0")) {
                duration = duration.substring(1);
            }
            int width = mc.font.width(duration);
            int textLeft = left + (24 - width) / 2;
            int textTop = y + 25;
            Primitives.fill(graphics.pose(), textLeft, textTop, textLeft + width, textTop + mc.font.lineHeight, -1873784752);
            graphics.drawString(mc.font, duration, textLeft, textTop, 16777215);

            left += 25;
        }
    }
}