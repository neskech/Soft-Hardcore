package net.ness.softhardcore.ui;

import net.minecraft.client.MinecraftClient;
import net.ness.softhardcore.SoftHardcoreClient;

/**
 * A scaling system that calculates UI component dimensions relative to their parent containers
 * instead of the entire screen. This allows for more flexible and responsive UI layouts.
 */
public class ScalingSystem {
    
    /**
     * Represents the dimensions of a container (parent or screen)
     */
    public static class ContainerDimensions {
        public final int width;
        public final int height;
        
        public ContainerDimensions(int width, int height) {
            this.width = width;
            this.height = height;
        }
        
        /**
         * Creates a container dimensions object for the entire screen
         */
        public static ContainerDimensions screen() {
            MinecraftClient client = SoftHardcoreClient.getClient();
            return new ContainerDimensions(
                client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight()
            );
        }
        
        /**
         * Creates a container dimensions object for a specific area
         */
        public static ContainerDimensions of(int width, int height) {
            return new ContainerDimensions(width, height);
        }
    }
    
    /**
     * Calculates a dimension as a percentage of the parent container's width
     */
    public static int widthPercent(ContainerDimensions parent, float percent) {
        return (int) (parent.width * percent);
    }
    
    /**
     * Calculates a dimension as a percentage of the parent container's height
     */
    public static int heightPercent(ContainerDimensions parent, float percent) {
        return (int) (parent.height * percent);
    }
    
    /**
     * Calculates a dimension as a percentage of the parent container's width, with a minimum value
     */
    public static int widthPercentMin(ContainerDimensions parent, float percent, int min) {
        return Math.max(min, widthPercent(parent, percent));
    }
    
    /**
     * Calculates a dimension as a percentage of the parent container's height, with a minimum value
     */
    public static int heightPercentMin(ContainerDimensions parent, float percent, int min) {
        return Math.max(min, heightPercent(parent, percent));
    }
    
    /**
     * Calculates a dimension as a percentage of the parent container's width, with a maximum value
     */
    public static int widthPercentMax(ContainerDimensions parent, float percent, int max) {
        return Math.min(max, widthPercent(parent, percent));
    }
    
    /**
     * Calculates a dimension as a percentage of the parent container's height, with a maximum value
     */
    public static int heightPercentMax(ContainerDimensions parent, float percent, int max) {
        return Math.min(max, heightPercent(parent, percent));
    }
    
    /**
     * Calculates a dimension as a percentage of the parent container's width, clamped between min and max
     */
    public static int widthPercentClamped(ContainerDimensions parent, float percent, int min, int max) {
        return Math.max(min, Math.min(max, widthPercent(parent, percent)));
    }
    
    /**
     * Calculates a dimension as a percentage of the parent container's height, clamped between min and max
     */
    public static int heightPercentClamped(ContainerDimensions parent, float percent, int min, int max) {
        return Math.max(min, Math.min(max, heightPercent(parent, percent)));
    }
    
    /**
     * Calculates a dimension based on the smaller of width or height percentage
     */
    public static int minDimensionPercent(ContainerDimensions parent, float percent) {
        return (int) (Math.min(parent.width, parent.height) * percent);
    }
    
    /**
     * Calculates a dimension based on the larger of width or height percentage
     */
    public static int maxDimensionPercent(ContainerDimensions parent, float percent) {
        return (int) (Math.max(parent.width, parent.height) * percent);
    }
    
    /**
     * Calculates a dimension that maintains aspect ratio relative to the parent
     */
    public static int aspectRatioDimension(ContainerDimensions parent, float aspectRatio, boolean useWidth) {
        if (useWidth) {
            return (int) (parent.width * aspectRatio);
        } else {
            return (int) (parent.height / aspectRatio);
        }
    }
}
