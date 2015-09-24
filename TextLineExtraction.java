package ch.unifr.diuf.diva.gabor;

import ch.unifr.diuf.diva.gabor.DrawGT;
import ij.ImagePlus;
import ij.blob.ManyBlobs;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is to: (1) create image annotated with wrapping polygons. If the CC is higher
 * than a threshold, it is considered as touched CC and a rectangle wrapping the CC is drawn.
 * (2) The touched CCs are extracted from the image. They will be processed later.
 *
 * @author WeiH
 */
public class TextLineExtraction {

    public String pathName;
    public String fileName;
    public static String originalName;
//	public static String originalName = "SaintGall_GaborInput.422.412.png";

    public Map<String, Rectangle> patches = new LinkedHashMap<>();
    List<Polygon> polygonsGT;
    public static int linkingRectHeight = 0;
    public static int linkingRectWidth = 0;

    public List<Polygon> start(int offsetX, int offsetY, Info info) {
        pathName = info.filePath;
        fileName = info.gaborOutput;
        originalName = info.gaborInput;
        BufferedImage img = null;
        BufferedImage imgOriginal = null;
        try {
            img = ImageIO.read(new File(pathName + fileName));
            imgOriginal = ImageIO.read(new File(pathName + originalName));
        } catch (IOException e) {
        }
        img = CommonFunctions.convertImage(img);
        drawCCs(img, imgOriginal);
        segmentCCs(pathName, img);
        linkCCs(pathName, originalName, img, info);
        polygonsGT = drawGT(pathName, info);
        offsetGT(offsetX, offsetY);
//        System.out.println("Done!");
        return polygonsGT;
    }


    /**
     * (1) Draw polygons and rectangles. (2) Extract touched CCs.
     *
     * @param img
     */
    public void drawCCs(BufferedImage img, BufferedImage imgOriginal) {
        ImagePlus imp = new ImagePlus("test", img);
        ManyBlobs allBlobs = new ManyBlobs(imp); // Extended ArrayList
        allBlobs.findConnectedComponents(); // Start the Connected Component
//        System.out.println(allBlobs.get(0).getPerimeter()); // Read the perimeter of a Blob
//        System.out.println("The number of CCs is: " + allBlobs.size());
//		imp.show();

        BufferedImage bufferedImage = new BufferedImage(imgOriginal.getWidth(), imgOriginal.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.drawImage(imgOriginal, 0, 0, null);
        g2d.setColor(Color.red);
        Rectangle rect = new Rectangle();
        for (int i = 0; i < allBlobs.size(); i++) {
            Polygon adjustedPolygon = CommonFunctions.adjustPolygon(allBlobs.get(i).getOuterContour());
            //	adjustedPolygon.translate(17, 17);
            g2d.drawPolygon(adjustedPolygon);
            rect = adjustedPolygon.getBounds();
            if (rect.height > 60) {
                g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
                createPatch(adjustedPolygon, i, img);
                patches.put("patch" + i + ".png", rect);
            }
        }
//        System.out.println("The number of patches is: " + patches.size());
        g2d.dispose();

        try {
            // Save as PNG
            File file = new File(pathName + "imageWithBigRect.png");
            if (file.exists()) {
                file.delete();
            }
            ImageIO.write(bufferedImage, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        System.out.println("The number of CCs is: " + allBlobs.size());
    }

    /**
     * Extract touched CCs.
     *
     * @param p
     * @param index
     * @param img
     */
    public void createPatch(Polygon p, int index, BufferedImage img) {
        Rectangle rect = p.getBounds();
        BufferedImage bufferedImage = new BufferedImage(rect.width + 1, rect.height + 1,
                BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = bufferedImage.getRaster();

        for (int i = 0; i < bufferedImage.getHeight(); i++) {
            for (int j = 0; j < bufferedImage.getWidth(); j++) {
                // The function "Polygon.contains" has a problem. It does not
                // include some points on the boundary.
                // Thus, the boundary points should be dealt with additionally.
                /*
                 * if ((p.contains(j + rect.x, i+rect.y) &&
				 * img.getRaster().getSample(j + rect.x, i+rect.y, 0)==0) ||
				 * boundaryPoints.contains(new Point(j + rect.x, i+rect.y)))
				 */
                if (j + rect.x >= 0 && i + rect.y >= 0) {
                    if (p.contains(j + rect.x, i + rect.y)
                            && img.getRaster().getSample(j + rect.x, i + rect.y, 0) == 0) {
                        raster.setSample(j, i, 0, 0);
                    } else {
                        raster.setSample(j, i, 0, 255);
                    }
                }
            }
        }

        try {
            File file = new File(pathName + "patch" + index + ".png");
            if (file.exists()) {
                file.delete();
            }
            ImageIO.write(bufferedImage, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        System.out.println("Patch" + index + " was generated.");
    }

    /**
     * This function is to transform the separating points in patches to original image, draw a RGB
     * image showing the separating lines and rectangles, and draw a gray image for next processing.
     *
     * @param img
     */
    public void segmentCCs(String pathName, BufferedImage img) {
        // transform the separating points in the patches to that of the original image
//        System.out.println("patches size: " + patches.size());
        Projection pj = new Projection(pathName);
        ArrayList<Point> separatingPointsInOriginal = new ArrayList<Point>();
        for (String patchName : patches.keySet()) {
//            System.out.println("The patch " + patchName + " is being processed.");
            Rectangle rect = patches.get(patchName);
            List<Point> separatingPointsInPatch = pj.start(patchName);
            for (Point p : separatingPointsInPatch) {
                Point pOriginal = new Point(p.x + rect.x, p.y + rect.y);
                separatingPointsInOriginal.add(pOriginal);
            }
        }

        // draw the separating points and rectangles wrapping the polygons and then create an image for show
        BufferedImage imgShow = new BufferedImage(img.getWidth(), img.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = imgShow.createGraphics();
        g2d.drawImage(img, 0, 0, null);
        g2d.setColor(Color.red);
        for (String patchName : patches.keySet()) {
            Rectangle rect = patches.get(patchName);
            g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
        }
        Color clr = Color.green;
        for (Point p : separatingPointsInOriginal) {
            if (p.x < imgShow.getWidth() && p.y < imgShow.getHeight()
                    && p.x >= 0 && p.y >= 0) {
                imgShow.setRGB(p.x, p.y, clr.getRGB());
            }
        }
        g2d.dispose();
        try {
            File file = new File(pathName + "segmentationProjectionShow.png");
            if (file.exists()) {
                file.delete();
            }
            ImageIO.write(imgShow, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // draw the separating points on the original gray image and then create the image for next processing
        WritableRaster raster = img.getRaster();
        for (Point p : separatingPointsInOriginal) {
            if (p.x < imgShow.getWidth() && p.y < imgShow.getHeight()
                    && p.x >= 0 && p.y >= 0) {
                raster.setSample(p.x, p.y, 0, 255);
            }
        }
        try {
            File file = new File(pathName + "segmentationProjectionNext.png");
            if (file.exists()) {
                file.delete();
            }
            ImageIO.write(img, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void linkCCs(String pathName, String originalName, BufferedImage img, Info info) {
//		LinkCCs linkCCs = new LinkCCs(pathName, originalName);
        LinkCCsV2 linkCCs = new LinkCCsV2(pathName, originalName, info);
        linkCCs.start(img, info);
    }

    public List<Polygon> drawGT(String pathName, Info info) {
        DrawGT drawGT = new DrawGT(pathName);
        return drawGT.start(info);
    }

    public void offsetGT(int x, int y) {
        for (Polygon polygon : polygonsGT) {
            // move to the coordinates on the original image
            // +1 is optional. It is probably due to the problem of the coordinates of ijblob polygon.
            polygon.translate(x + 1, y);
        }
    }
}
