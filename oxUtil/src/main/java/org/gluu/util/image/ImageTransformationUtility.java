/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util.image;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

/**
 * Utility to do high quality image resize
 *
 * @author Yuriy Movchan Date: 11.03.2010
 */
public final class ImageTransformationUtility {

    private ImageTransformationUtility() { }

    /**
     *
     * @param img
     *            the original image to be scaled
     * @param maxWidth
     *            the desired width of the scaled instance, in pixels
     * @param maxHeight
     *            the desired height of the scaled instance, in pixels
     * @return a scaled version of the original {@code BufferedImage}
     */
    public static BufferedImage scaleImage(BufferedImage img, int maxWidth, int maxHeight) {
        return scaleImage(img, maxWidth, maxHeight, RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
    }

    /**
     * Convenience method that returns a scaled instance of the provided
     * {@code BufferedImage}.
     *
     * @param img
     *            the original image to be scaled
     * @param maxWidth
     *            the desired width of the scaled instance, in pixels
     * @param maxHeight
     *            the desired height of the scaled instance, in pixels
     * @param hint
     *            one of the rendering hints that corresponds to
     *            {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     *            {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     *            {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     *            {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality
     *            if true, this method will use a multi-step scaling technique that
     *            provides higher quality than the usual one-step technique (only
     *            useful in downscaling cases, where {@code targetWidth} or
     *            {@code targetHeight} is smaller than the original dimensions, and
     *            generally only when the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    public static BufferedImage scaleImage(BufferedImage img, int maxWidth, int maxHeight, Object hint,
            boolean higherQuality) {
        BufferedImage ret = img;
        if ((img.getWidth() <= maxWidth) && (img.getHeight() <= maxHeight)) {
            return ret;
        }

        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;

        int targetWidth = img.getWidth();
        int targetHeight = img.getHeight();
        double wRatio = (double) targetWidth / (double) maxWidth;
        double hRatio = (double) targetHeight / (double) maxHeight;
        if (wRatio > hRatio) {
            targetWidth = maxWidth;
            targetHeight = (int) (targetHeight / wRatio);
        } else {
            targetWidth = (int) (targetWidth / hRatio);
            targetHeight = maxHeight;
        }

        boolean downScale = img.getWidth() > maxWidth || img.getHeight() > maxHeight;

        int currentWidth, currentheight;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            currentWidth = img.getWidth();
            currentheight = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            currentWidth = targetWidth;
            currentheight = targetHeight;
        }

        do {
            if (downScale) {
                if (higherQuality && currentWidth > targetWidth) {
                    currentWidth /= 2;
                    if (currentWidth < targetWidth) {
                        currentWidth = targetWidth;
                    }
                }

                if (higherQuality && currentheight > targetHeight) {
                    currentheight /= 2;
                    if (currentheight < targetHeight) {
                        currentheight = targetHeight;
                    }
                }
            } else {
                currentWidth = targetWidth;
                currentheight = targetWidth;
            }

            BufferedImage tmp = new BufferedImage(currentWidth, currentheight, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, currentWidth, currentheight, null);
            g2.dispose();

            ret = tmp;
        } while (currentWidth != targetWidth || currentheight != targetHeight);

        return ret;

    }

}
