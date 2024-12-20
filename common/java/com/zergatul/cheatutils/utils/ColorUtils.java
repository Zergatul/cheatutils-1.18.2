package com.zergatul.cheatutils.utils;

import java.awt.*;
import java.util.Locale;

public class ColorUtils {

    public static Integer parseColor(String str) {
        if (str == null) {
            return null;
        }
        str = str.toLowerCase(Locale.ROOT);
        if (str.length() == 7) {
            if (str.charAt(0) != '#') {
                return null;
            }
            for (int i = 1; i < 7; i++) {
                char ch = str.charAt(i);
                if ('0' <= ch && ch <= '9') {
                    continue;
                }
                if ('a' <= ch && ch <= 'f') {
                    continue;
                }
                return null;
            }
            return  Integer.parseInt(str.substring(1, 3), 16) << 16 |
                    Integer.parseInt(str.substring(3, 5), 16) << 8 |
                    Integer.parseInt(str.substring(5, 7), 16);
        }
        if (str.length() == 9) {
            if (str.charAt(0) != '#') {
                return null;
            }
            for (int i = 1; i < 9; i++) {
                char ch = str.charAt(i);
                if ('0' <= ch && ch <= '9') {
                    continue;
                }
                if ('a' <= ch && ch <= 'f') {
                    continue;
                }
                return null;
            }
            return  Integer.parseInt(str.substring(7, 9), 16) << 24 |
                    Integer.parseInt(str.substring(1, 3), 16) << 16 |
                    Integer.parseInt(str.substring(3, 5), 16) << 8 |
                    Integer.parseInt(str.substring(5, 7), 16);
        }
        return null;
    }

    public static Color parseColor2(String str) {
        if (str == null) {
            return null;
        }
        str = str.toLowerCase(Locale.ROOT);
        if (str.length() == 7) {
            if (str.charAt(0) != '#') {
                return null;
            }
            for (int i = 1; i < 7; i++) {
                char ch = str.charAt(i);
                if ('0' <= ch && ch <= '9') {
                    continue;
                }
                if ('a' <= ch && ch <= 'f') {
                    continue;
                }
                return null;
            }
            return new Color(
                    Integer.parseInt(str.substring(1, 3), 16),
                    Integer.parseInt(str.substring(3, 5), 16),
                    Integer.parseInt(str.substring(5, 7), 16));
        }
        if (str.length() == 9) {
            if (str.charAt(0) != '#') {
                return null;
            }
            for (int i = 1; i < 9; i++) {
                char ch = str.charAt(i);
                if ('0' <= ch && ch <= '9') {
                    continue;
                }
                if ('a' <= ch && ch <= 'f') {
                    continue;
                }
                return null;
            }
            return new Color(
                    Integer.parseInt(str.substring(1, 3), 16),
                    Integer.parseInt(str.substring(3, 5), 16),
                    Integer.parseInt(str.substring(5, 7), 16),
                    Integer.parseInt(str.substring(7, 9), 16));
        }
        return null;
    }

    public static Color inverse(Color color) {
        return new Color(
                255 - color.getRed(),
                255 - color.getGreen(),
                255 - color.getBlue(),
                color.getAlpha());
    }

    public static int inverse(int color) {
        int a = color >> 24 & 255;
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
        return (a << 24) | ((255 - r) << 16) | ((255 - g) << 8) | (255 - b);
    }
}