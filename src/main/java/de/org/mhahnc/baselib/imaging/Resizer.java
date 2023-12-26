package de.org.mhahnc.baselib.imaging;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public class Resizer {
    public static class Options {
        /** The resolution. No default available. */
        public Resolution resolution = null;

        /** True (default) if the image should be enlarged if it is smaller
         * than the actual target resolution or false if not. */
        public boolean resizeSmaller = true;

        /** True if the gamma level of the target image should be automatically
         * adjusted (to make it brighter or darker) or false if not. */
        public boolean autoGamma = false;

        /**
         * Rotation levels. If an image's aspect ratio is inverse to the target
         * resolution then it will fill more space (and get a better resolution)
         * if it's rotated. Rotation can be turned off or either 90 degrees to
         * the left (default) or to the right.
         */
        public enum Rotate {
            OFF,
            LEFT,
            RIGHT;
        }
        public Rotate rotate = Rotate.LEFT;

        /**
         * Background color for the slack space, since the aspect ratios of
         * pictures are always preserved.
         */
        public Color backgroundColor = Color.BLACK;
    }

    /**
     * Target image parameters, calculated at runtime, used by the processor to
     * determine the transformations to create the final image. Which, because
     * of the aspect ratio keeping, can have different shape than the actual
     * target where it is rendered into..
     */
    static class TargetImageParams {
        /** Width the image must have. */
        public int width;
        /** Height the image must have. */
        public int height;
        /** Horizontal position the (0,0) coordinate of the image must be
         * located in the target. */
        public int x;
        /** Vertical position the (0,0) coordinate of the image must be located
         * in the target. */
        public int y;
        /** The interpolation time to be used, depending if the image needs to
         * be down- or up-sized. */
        public int interpolationType;
        /** The scale factor by the image needs to be enlarged or shrunk. */
        double scale;
        /** True if the image needs to be rotated or false if not. */
        boolean rotate;
    }

    /**
     * Determines everything needed to create the target image.
     * @param img The original image.
     * @return The target parameters.
     */
    TargetImageParams calculateTargetImage(BufferedImage img) {
        TargetImageParams result = new TargetImageParams();

        int w = img.getWidth();
        int h = img.getHeight();
        int rw = this.options.resolution.width;
        int rh = this.options.resolution.height;

        double a = (double)w / (double)Math.max(h, 1);
        double ra = (double)rw / (double)Math.max(rh, 1);

        boolean larger = w > rw || h > rh;

        if (Options.Rotate.OFF != this.options.rotate &&
            ((ra > 1.0 && a < 1.0) ||
             (ra < 1.0 && a > 1.0)) &&
             (larger || this.options.resizeSmaller)) {
            result.rotate = true;
            int swp = w;
            w = h;
            h = swp;
            a = (double)w / (double)Math.max(h, 1);
        }

        result.interpolationType = larger ? AffineTransformOp.TYPE_BICUBIC :
                                            AffineTransformOp.TYPE_BILINEAR;

        if (!this.options.resizeSmaller && !larger) {
            result.width = w;
            result.height = h;
        }
        else {
            if (a > ra) {
                result.width = rw;
                result.height = (int)(rw / (0.0 == a ? 1 : a));
            }
            else {
                result.width = (int)(rh * a);
                result.height = rh;
            }
        }

        result.x = rw - result.width >> 1;
        result.y = rh - result.height >> 1;

        // FIXME: explain why we need to add one here to avoid a small gap
        result.scale = ((double)result.width + 1) / Math.max(w, 1);

        return result;
    }

    Options options;

    public Resizer(Options options) {
        this.options = options;

    }

    public BufferedImage resize(BufferedImage img) {
        TargetImageParams tip = calculateTargetImage(img);

        AffineTransform atrans = AffineTransform.getScaleInstance(tip.scale, tip.scale);

        if (tip.rotate) {
            boolean rr = Options.Rotate.RIGHT == this.options.rotate;
            atrans.rotate(rr ? Math.PI / 2 :
                              -Math.PI / 2);
            atrans.translate(
                    rr ? 0 : -img.getWidth(),
                    rr ? -img.getHeight() : 0);
        }

        AffineTransformOp atop = new AffineTransformOp(
                atrans,
                tip.interpolationType);

        BufferedImage timg = new BufferedImage(tip.width, tip.height, img.getType());

        atop.filter(img, timg);
        img = null;

        BufferedImage result = new BufferedImage(
                this.options.resolution.width,
                this.options.resolution.height,
                timg.getType());

        Graphics grph = result.getGraphics();

        grph.setColor(this.options.backgroundColor);
        grph.fillRect(0, 0, result.getWidth(), result.getHeight());

        grph.drawImage(timg, tip.x, tip.y, null);
        grph.dispose();

        return result;
    }
}
