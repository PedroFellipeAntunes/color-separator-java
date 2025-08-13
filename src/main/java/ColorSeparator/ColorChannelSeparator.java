package ColorSeparator;

import java.awt.image.BufferedImage;

public class ColorChannelSeparator {
    /**
     * Separates the input image into its Red, Green, Blue, and Alpha channels.
     *
     * @param image The input BufferedImage to be processed.
     * @param offset Contrast adjustment value. Positive to increase, negative
     * to decrease contrast.
     * @param invert If true, inverts the color values before processing.
     * @param colored If true, outputs the channels in their respective color
     * components; otherwise outputs them in grayscale.
     * @return An array of 4 BufferedImages representing the Red, Green, Blue,
     * and Alpha channels.
     */
    public BufferedImage[] separateRBGA(BufferedImage image, int offset, boolean invert, boolean colored) {
        BufferedImage redChannel = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        BufferedImage greenChannel = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        BufferedImage blueChannel = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        BufferedImage alphaChannel = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixelColor = image.getRGB(x, y);
                
                int alpha = (pixelColor >> 24) & 0xff;
                int red = (pixelColor >> 16) & 0xff;
                int green = (pixelColor >> 8) & 0xff;
                int blue = pixelColor & 0xff;
                
                int newAlpha = alpha;
                
                if (invert) {
                    red = 255 - red;
                    green = 255 - green;
                    blue = 255 - blue;
                    newAlpha = 255 - alpha;
                }
                
                red = applyContrast(red, offset);
                green = applyContrast(green, offset);
                blue = applyContrast(blue, offset);
                alpha = applyContrast(alpha, offset);

                int redPixel = colored ? ((alpha << 24) | (red << 16)) 
                                       : ((alpha << 24) | (red << 16) | (red << 8) | red);
                
                int greenPixel = colored ? ((alpha << 24) | (green << 8)) 
                                         : ((alpha << 24) | (green << 16) | (green << 8) | green);
                
                int bluePixel = colored ? ((alpha << 24) | blue) 
                                        : ((alpha << 24) | (blue << 16) | (blue << 8) | blue);
                
                //Always max alpha so it can be used as a transparency mask
                int alphaPixel = (255 << 24) | (newAlpha << 16) | (newAlpha << 8) | newAlpha;

                redChannel.setRGB(x, y, redPixel);
                greenChannel.setRGB(x, y, greenPixel);
                blueChannel.setRGB(x, y, bluePixel);
                alphaChannel.setRGB(x, y, alphaPixel);
            }
        }
        
        return new BufferedImage[]{redChannel, greenChannel, blueChannel, alphaChannel};
    }
    
    /**
    * Separates the input image into its Cyan, Magenta, Yellow, and Black (CMYK) channels.
    * 
    * @param image The input BufferedImage to be processed.
    * @param offset Contrast adjustment value. Positive to increase, negative to
    * decrease contrast.
    * @param invert If true, inverts the CMYK channel values after conversion.
    * @param colored If true, outputs the channels using distinct color
    * representations through a screen blend of the grayscale output.
    * @return An array of 4 BufferedImages representing the Cyan, Magenta, Yellow, and Black channels.
    */
    public BufferedImage[] separateCMYK(BufferedImage image, int offset, boolean invert, boolean colored) {
        BufferedImage cyanChannel = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        BufferedImage magentaChannel = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        BufferedImage yellowChannel = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        BufferedImage blackChannel = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixelColor = image.getRGB(x, y);

                int alpha = (pixelColor >> 24) & 0xff;
                double red = ((pixelColor >> 16) & 0xff) / 255.0;
                double green = ((pixelColor >> 8) & 0xff) / 255.0;
                double blue = (pixelColor & 0xff) / 255.0;
                
                double k = 1.0 - Math.max(red, Math.max(green, blue));
                double divisor = 1.0 - k;

                if (divisor == 0.0) {
                    divisor = 1.0;
                }
                
                double c = (1.0 - red - k) / divisor;
                double m = (1.0 - green - k) / divisor;
                double yVal = (1.0 - blue - k) / divisor;

                int cyan = 255 - (int) (c * 255);
                int magenta = 255 - (int) (m * 255);
                int yellow = 255 - (int) (yVal * 255);
                int black = 255 - (int) (k * 255);

                if (invert) {
                    cyan = 255 - cyan;
                    magenta = 255 - magenta;
                    yellow = 255 - yellow;
                    black = 255 - black;
                }

                cyan = applyContrast(cyan, offset);
                magenta = applyContrast(magenta, offset);
                yellow = applyContrast(yellow, offset);
                black = applyContrast(black, offset);

                int cyanPixel, magentaPixel, yellowPixel, blackPixel;

                if (colored) {
                    int screenR_C = screenBlend(cyan, 0);
                    int screenG_C = screenBlend(cyan, 255);
                    int screenB_C = screenBlend(cyan, 255);
                    cyanPixel = (alpha << 24) | (screenR_C << 16) | (screenG_C << 8) | screenB_C;

                    int screenR_M = screenBlend(magenta, 255);
                    int screenG_M = screenBlend(magenta, 0);
                    int screenB_M = screenBlend(magenta, 255);
                    magentaPixel = (alpha << 24) | (screenR_M << 16) | (screenG_M << 8) | screenB_M;
                    
                    int screenR_Y = screenBlend(yellow, 255);
                    int screenG_Y = screenBlend(yellow, 255);
                    int screenB_Y = screenBlend(yellow, 0);
                    yellowPixel = (alpha << 24) | (screenR_Y << 16) | (screenG_Y << 8) | screenB_Y;
                } else {
                    cyanPixel = (alpha << 24) | (cyan << 16) | (cyan << 8) | cyan;
                    magentaPixel = (alpha << 24) | (magenta << 16) | (magenta << 8) | magenta;
                    yellowPixel = (alpha << 24) | (yellow << 16) | (yellow << 8) | yellow;
                }
                
                blackPixel = (alpha << 24) | (black << 16) | (black << 8) | black; // Always grayscale

                cyanChannel.setRGB(x, y, cyanPixel);
                magentaChannel.setRGB(x, y, magentaPixel);
                yellowChannel.setRGB(x, y, yellowPixel);
                blackChannel.setRGB(x, y, blackPixel);
            }
        }
        
        return new BufferedImage[]{cyanChannel, magentaChannel, yellowChannel, blackChannel};
    }
    
    private int applyContrast(int value, int contrast) {
        double factor = (259.0 * (contrast + 255)) / (255.0 * (259 - contrast));
        
        return Math.min(255, Math.max(0, (int)(factor * (value - 128) + 128)));
    }
    
    private int screenBlend(int gray, int pure) {
        double nGray = gray / 255.0;
        double nPure = pure / 255.0;

        double nOut = 1.0 - (1.0 - nGray) * (1.0 - nPure);
        int out = (int) Math.round(nOut * 255);
        
        out = Math.max(0, Math.min(255, out));
        
        return out;
    }
}