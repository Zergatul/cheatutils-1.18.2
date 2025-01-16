package com.zergatul.cheatutils.scripting.types;

import com.zergatul.scripting.Getter;
import com.zergatul.scripting.type.CustomType;
import net.minecraft.network.chat.Style;

@CustomType(name = "FormattedTextComponent")
public class FormattedTextComponent {

    private final String text;
    private final Style style;

    public FormattedTextComponent(String text, Style style) {
        this.text = text;
        this.style = style;
    }

    @Getter(name = "text")
    public String getText() {
        return text;
    }

    @Getter(name = "style")
    public StyleWrapper getStyle() {
        return new StyleWrapper(style);
    }
}