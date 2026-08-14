package com.alkacode.crates.animation;

/** Transform de um display entity: offset, rotation e scale. */
public final class Transform {

    private final double[] offset;
    private final double[] rotation;
    private final double[] scale;

    public Transform(double[] offset, double[] rotation, double[] scale) {
        this.offset = offset;
        this.rotation = rotation;
        this.scale = scale;
    }

    public static Transform identity() {
        return new Transform(new double[]{0, 0, 0}, new double[]{0, 0, 0}, new double[]{1, 1, 1});
    }

    public double[] getOffset() { return offset; }
    public double[] getRotation() { return rotation; }
    public double[] getScale() { return scale; }
}
