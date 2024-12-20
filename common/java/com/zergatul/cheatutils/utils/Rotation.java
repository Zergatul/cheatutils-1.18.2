package com.zergatul.cheatutils.utils;

public record Rotation(float xRot, float yRot) {

    public Rotation addXRot(float delta) {
        return new Rotation(xRot + delta, yRot);
    }

    public Rotation addYRot(float delta) {
        return new Rotation(xRot, yRot + delta);
    }

    public Rotation withXRot(float xRot) {
        return new Rotation(xRot, yRot);
    }

    public boolean approximateEquals(Rotation other, float epsilon) {
        return Math.abs(xRot - other.xRot) + Math.abs(yRot - other.yRot) < epsilon;
    }

    public float distanceSqrTo(Rotation other) {
        float dxRot = xRot - other.xRot;
        float dyRot = (yRot - other.yRot) % 360;
        if (dyRot < -180) {
            dyRot += 360;
        }
        if (dyRot > 180) {
            dyRot -= 360;
        }
        return dxRot * dxRot + dyRot * dyRot;
    }
}