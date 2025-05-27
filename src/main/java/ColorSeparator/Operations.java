package ColorSeparator;

import FileManager.PngReader;
import FileManager.PngSaver;
import Windows.ImageViewerGrid;

import java.awt.image.BufferedImage;

public class Operations {
    private final int offset;
    private final boolean invert;
    private final TYPE type;
    private final boolean colored;

    /**
     * Initializes the Operations instance with the desired configuration.
     *
     * @param offset Pixel offset to apply.
     * @param invert Whether to invert channels.
     * @param type Channel separation type (RGBA or CMYK).
     * @param colored Whether to produce colored output.
     */
    public Operations(int offset, boolean invert, TYPE type, boolean colored) {
        this.offset = offset;
        this.invert = invert;
        this.type = type;
        this.colored = colored;
    }

    /**
     * Executes the full channel separation pipeline.
     *
     * @param filePath Path to the image file to be processed.
     */
    public void processFile(String filePath) {
        BufferedImage input = measureTime("Reading file", () ->
            new PngReader().readPNG(filePath, false)
        );

        BufferedImage[] channels = measureTime("Separating channels: " + type, () -> {
            ColorChannelSeparator sep = new ColorChannelSeparator();
            
            if (type == TYPE.RGBA) {
                return sep.separateRBGA(input, offset, invert, colored);
            } else {
                return sep.separateCMYK(input, offset, invert, colored);
            }
        });
        
        ImageViewerGrid ivg = new ImageViewerGrid(channels, filePath, this);
    }

    /**
     * Saves each channel image to a file with an appropriate prefix.
     *
     * @param images   Array of channel images.
     * @param filePath The original file path used as a base for the saved files.
     */
    public void saveImages(BufferedImage[] images, String filePath) {
        PngSaver saver = new PngSaver();
        
        if (type == TYPE.RGBA) {
            saver.saveToFile("RGBA[" + invert + "," + colored + "]_Red",   filePath, images[0]);
            saver.saveToFile("RGBA[" + invert + "," + colored + "]_Green", filePath, images[1]);
            saver.saveToFile("RGBA[" + invert + "," + colored + "]_Blue",  filePath, images[2]);
            saver.saveToFile("RGBA[" + invert + "," + colored + "]_Alpha", filePath, images[3]);
        } else {
            saver.saveToFile("CMYK[" + invert + "," + colored + "]_Cyan",    filePath, images[0]);
            saver.saveToFile("CMYK[" + invert + "," + colored + "]_Magenta", filePath, images[1]);
            saver.saveToFile("CMYK[" + invert + "," + colored + "]_Yellow",  filePath, images[2]);
            saver.saveToFile("CMYK[" + invert + "," + colored + "]_Black",   filePath, images[3]);
        }
    }

    // ——— Helpers for timing measurements ——————————————————————————————
    
    private <T> T measureTime(String label, Timeable<T> action) {
        long start = System.currentTimeMillis();
        System.out.println(label);
        T result = action.execute();
        System.out.println("TIME: " + (System.currentTimeMillis() - start) + "ms");
        
        return result;
    }

    @FunctionalInterface
    private interface Timeable<T> {
        T execute();
    }
}