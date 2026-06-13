package net.ness.softhardcore.ui;

import net.minecraft.client.Minecraft;
import net.ness.softhardcore.SoftHardcoreClient;

public class ScalingSystem {

    public static class ContainerDimensions {
        public final int width;
        public final int height;

        public ContainerDimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public static ContainerDimensions screen() {
            Minecraft client = SoftHardcoreClient.getClient();
            return new ContainerDimensions(
                    client.getWindow().getGuiScaledWidth(),
                    client.getWindow().getGuiScaledHeight()
            );
        }

        public static ContainerDimensions of(int width, int height) {
            return new ContainerDimensions(width, height);
        }
    }

    public static int widthPercent(ContainerDimensions parent, float percent) {
        return (int) (parent.width * percent);
    }

    public static int heightPercent(ContainerDimensions parent, float percent) {
        return (int) (parent.height * percent);
    }

    public static int widthPercentMin(ContainerDimensions parent, float percent, int min) {
        return Math.max(min, widthPercent(parent, percent));
    }

    public static int heightPercentMin(ContainerDimensions parent, float percent, int min) {
        return Math.max(min, heightPercent(parent, percent));
    }

    public static int widthPercentMax(ContainerDimensions parent, float percent, int max) {
        return Math.min(max, widthPercent(parent, percent));
    }

    public static int heightPercentMax(ContainerDimensions parent, float percent, int max) {
        return Math.min(max, heightPercent(parent, percent));
    }

    public static int widthPercentClamped(ContainerDimensions parent, float percent, int min, int max) {
        return Math.max(min, Math.min(max, widthPercent(parent, percent)));
    }

    public static int heightPercentClamped(ContainerDimensions parent, float percent, int min, int max) {
        return Math.max(min, Math.min(max, heightPercent(parent, percent)));
    }

    public static int minDimensionPercent(ContainerDimensions parent, float percent) {
        return (int) (Math.min(parent.width, parent.height) * percent);
    }

    public static int maxDimensionPercent(ContainerDimensions parent, float percent) {
        return (int) (Math.max(parent.width, parent.height) * percent);
    }

    public static int aspectRatioDimension(ContainerDimensions parent, float aspectRatio, boolean useWidth) {
        if (useWidth) {
            return (int) (parent.width * aspectRatio);
        } else {
            return (int) (parent.height / aspectRatio);
        }
    }
}
