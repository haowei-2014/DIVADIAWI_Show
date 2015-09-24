package ch.unifr.diuf.diva.gabor.modification;

import ch.unifr.diuf.diva.gabor.CommonFunctions;
import ch.unifr.diuf.diva.gabor.MyPoint;
import ij.ImagePlus;
import ij.blob.Blob;
import ij.blob.ManyBlobs;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * This class is used to merge two polygons which belong to the same text line.
 * 
 * @author hao
 *
 */
public class MergePolygons {
    public static BufferedImage image;
    public static Rectangle polygonBound1;
    public static Rectangle polygonBound2;
    public static Polygon pNew;  // final single polygon
    public static int xOffset;
    public static int yOffset;
    public static int width;
    public static int height;
    public static Polygon newPolygon1;
    public static Polygon newPolygon2;
    public static Polygon leftPolygon;
    public static Polygon rightPolygon;
    public static List<Polygon> polygonsGT = new ArrayList<Polygon>();
    public static Map<String, List<int[][]>> results = new HashMap<String, List<int[][]>>();

    protected MergePolygons() {
    }

    /**
     * Generate an image containing the two polygons
     * 
     * @param myPoints1
     * @param myPoints2
     */
    public static void generatePolygonImage(MyPoint[] myPoints1, MyPoint[] myPoints2) {
        // Based on the points, construct the two polygons.
        int[] xpoints1 = new int[myPoints1.length];
        int[] ypoints1 = new int[myPoints1.length];
        for (int i = 0; i < myPoints1.length; i++) {
            xpoints1[i] = myPoints1[i].x;
            ypoints1[i] = myPoints1[i].y;
        }
        Polygon polygon1 = new Polygon(xpoints1, ypoints1, myPoints1.length);
        polygonBound1 = polygon1.getBounds();

        int[] xpoints2 = new int[myPoints2.length];
        int[] ypoints2 = new int[myPoints2.length];
        for (int i = 0; i < myPoints2.length; i++) {
            xpoints2[i] = myPoints2[i].x;
            ypoints2[i] = myPoints2[i].y;
        }
        Polygon polygon2 = new Polygon(xpoints2, ypoints2, myPoints2.length);
        polygonBound2 = polygon2.getBounds();

        // compute the x, y, widht and height of the boundingbox containing the two polygons
        if (polygonBound1.x < polygonBound2.x) {
            xOffset = polygonBound1.x;
            width = polygonBound2.x + polygonBound2.width - polygonBound1.x;
        } else {
            xOffset = polygonBound2.x;
            width = polygonBound1.x + polygonBound1.width - polygonBound2.x;
        }
        if (polygonBound1.y < polygonBound2.y) {
            yOffset = polygonBound1.y;
        } else {
            yOffset = polygonBound2.y;
        }
        if (polygonBound1.y + polygonBound1.height > polygonBound2.y + polygonBound2.height) {
            height = polygonBound1.y + polygonBound1.height - yOffset;
        } else {
            height = polygonBound2.y + polygonBound2.height - yOffset;
        }
        // create two new polygons on a new image.
        int[] xNewPoints1 = new int[myPoints1.length];
        int[] yNewPoints1 = new int[myPoints1.length];
        for (int i = 0; i < myPoints1.length; i++) {
            xNewPoints1[i] = xpoints1[i] - xOffset;
            yNewPoints1[i] = ypoints1[i] - yOffset;

        }
        newPolygon1 = new Polygon(xNewPoints1, yNewPoints1, myPoints1.length);
        int[] xNewPoints2 = new int[myPoints2.length];
        int[] yNewPoints2 = new int[myPoints2.length];
        for (int i = 0; i < myPoints2.length; i++) {
            xNewPoints2[i] = xpoints2[i] - xOffset;
            yNewPoints2[i] = ypoints2[i] - yOffset;
        }
        newPolygon2 = new Polygon(xNewPoints2, yNewPoints2, myPoints2.length);

        // draw image and the two polygons.
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.setColor(Color.black);
        g2d.fillPolygon(newPolygon1);
        g2d.fillPolygon(newPolygon2);
//        System.out.println("mergegeneratePolygonImage is finished.");
    }

    /**
     * Merge these two polygons
     * 
     * @param leftPolygon
     * @param rightPolygon
     */
    public static void mergePolygons(Polygon leftPolygon, Polygon rightPolygon) {
        double ratio = 0.3; // a ratio of the width of the polygon
        int xLinkingRect = leftPolygon.getBounds().x + leftPolygon.getBounds().width;
        xLinkingRect -= leftPolygon.getBounds().width * ratio;
        int yLinkingRect = 0;
        int widthLinkingRect = rightPolygon.getBounds().x - (leftPolygon.getBounds().x + leftPolygon.getBounds().width);
        widthLinkingRect = (int) (widthLinkingRect + leftPolygon.getBounds().width * ratio
                + rightPolygon.getBounds().width * ratio);
        int heightLinkingRect = height;
        Rectangle linkingRect = new Rectangle(xLinkingRect, yLinkingRect, widthLinkingRect, heightLinkingRect);
        image = CommonFunctions.drawRectsMerge(image, leftPolygon, rightPolygon, linkingRect);

//        System.out.println("mergemergePolygons is finished.");
    }

    /**
     * Set a threshold, and generate only one polygon.
     */
    public static void generateMergedPolygon() {
        image = CommonFunctions.convertImage(image);
        ImagePlus imp = new ImagePlus("test", image);
        ManyBlobs allBlobs = new ManyBlobs(imp); // Extended ArrayList
        allBlobs.findConnectedComponents(); // Start the Connected Component
        allBlobs = allBlobs.filterBlobs(500, 100000000, Blob.GETENCLOSEDAREA);
//        System.out.println("AllBlobs size: " + allBlobs.size());
        if (allBlobs.size() != 1) {
            return;
        }
        pNew = CommonFunctions.adjustPolygon(allBlobs.get(0).getOuterContour());
        pNew = CommonFunctions.resamplePolygon(pNew);
        polygonsGT.add(pNew);

        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.black);
        g2d.fillPolygon(pNew);
//        System.out.println("mergegenerateMergedPolygon is finished.");
    }


    /**
     * 
     * Save the result to a HashMap
     * 
     * @param myPoints1
     * @param myPoints2
     * @return
     */
    public static Map<String, List<int[][]>> getResults(MyPoint[] myPoints1, MyPoint[] myPoints2) {
        polygonsGT.clear();
        results.clear();
        generatePolygonImage(myPoints1, myPoints2);
        if (newPolygon1.getBounds().x < newPolygon2.getBounds().x) {
            mergePolygons(newPolygon1, newPolygon2);
        } else {
            mergePolygons(newPolygon2, newPolygon1);
        }
        generateMergedPolygon();

        List<int[][]> mergePolygonsList = new ArrayList<int[][]>(); // actually only one polygon
        for (Polygon polygon : polygonsGT) {
            polygon.translate(xOffset, yOffset);
            int[][] points = new int[polygon.npoints][2];
            for (int i = 0; i < polygon.npoints; i++) {
                points[i][0] = polygon.xpoints[i];
                points[i][1] = polygon.ypoints[i];
            }
            mergePolygonsList.add(points);
        }
        results.put("textLines", mergePolygonsList);
        return results;
    }

}
