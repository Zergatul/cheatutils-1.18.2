package com.zergatul.cheatutils.scripting.types;

import com.zergatul.scripting.Getter;
import com.zergatul.scripting.Lazy;
import com.zergatul.scripting.type.CustomType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@CustomType(name = "FormattedText")
public class ComponentWrapper {

    private final Component inner;
    private final Lazy<FormattedTextComponent[]> components;

    public ComponentWrapper(Component component) {
        this.inner = component;
        this.components = new Lazy<>(this::getComponentsInternal);
    }

    @Getter(name = "text")
    public String getText() {
        return inner.getString();
    }

    @Getter(name = "components")
    public FormattedTextComponent[] getComponents() {
        return components.value();
    }

    private FormattedTextComponent[] getComponentsInternal() {
        List<FormattedTextComponent> components = new ArrayList<>();
        inner.visit((style, text) -> {
            components.add(new FormattedTextComponent(text, style));
            return Optional.empty();
        }, Style.EMPTY);
        return components.toArray(FormattedTextComponent[]::new);
    }
}