package ch.unifr.diuf.diva.gabor.modification;

import ch.unifr.diuf.diva.gabor.CommonFunctions;
import ch.unifr.diuf.diva.gabor.DrawGT;
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
 * This class is used to split vertically attached connected component into two lines.
 * 
 * @author hao
 *
 */
public class SplitPolygon {

    public static BufferedImage image;
    public static Rectangle polygonBound;
    public static Polygon pNew1;
    public static Polygon pNew2;
    public static List<Polygon> polygonsGT = new ArrayList<>();
    public static Map<String, List<int[][]>> results = new HashMap<>();

    protected SplitPolygon() {
    }

    /**
     * Generate an image containing the polygon.
     * 
     * @param points
     */
    public static void generatePolygonImage(MyPoint[] points) {
        int[] xpoints = new int[points.length];
        int[] ypoints = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            xpoints[i] = points[i].x;
            ypoints[i] = points[i].y;
        }
        Polygon polygon = new Polygon(xpoints, ypoints, points.length);
        polygonBound = polygon.getBounds();

        int[] xNewPoints = new int[points.length];
        int[] yNewPoints = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            xNewPoints[i] = xpoints[i] - polygonBound.x;
            yNewPoints[i] = ypoints[i] - polygonBound.y;
        }
        Polygon newPolygon = new Polygon(xNewPoints, yNewPoints, points.length);

        image = new BufferedImage(polygonBound.width, polygonBound.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d.setColor(Color.black);
        g2d.fillPolygon(newPolygon);

//		System.out.println("generatePolygonImage is finished.");
    }

    /**
     * Split the polygon into two new polygons.
     * 
     * @param xSplit
     * @param ySplit
     */
    public static void splitPolygon(int xSplit, int ySplit) {
        xSplit -= polygonBound.x;
        ySplit -= polygonBound.y;
        // height of the splitting rectangle
        int heightRect = 20;
        int incrementalWidth = 20;
        int xRect = xSplit - incrementalWidth / 2;
        int yRect = ySplit - heightRect / 2;
        int countIterations = 0; // sometimes the loop below becomes a dead loop, so we count the iterations.
        // If the count exceed a threshold, break the loop
        // increase the width of the linking rectangle until the polygon is split
        for (int widthRect = 60; widthRect < 1500; widthRect += incrementalWidth) {
            countIterations++;
            if (countIterations > 100) {
//				System.out.println("countIterations: " + countIterations);
                break;
            }
//			System.out.println("Splitting rectangle width: " + widthRect);
            xRect = xRect - incrementalWidth / 2;
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(Color.white);
            g2d.fillRect(xRect, yRect, widthRect, heightRect);
            if (xRect < 0) {
                xRect = 0;
            }
            if (yRect < 0) {
                yRect = 0;
            }
            if (xSplit + widthRect / 2 >= polygonBound.width) {
                widthRect = polygonBound.width - xRect;
            }
            if (ySplit + heightRect / 2 >= polygonBound.height) {
                heightRect = polygonBound.height - yRect;
            }
            g2d.fillRect(xRect, yRect, widthRect, heightRect);
//			g2d.fillRect(xSplit-5, ySplit-2, 10, 4);

            if (generate2NewPolygons()) {
                break;
            }
        }
//		System.out.println("splitPolygon is finished.");
    }

    /**
     * If successfully generate 2 new polygons, return true.
     * 
     * @return
     */
    public static boolean generate2NewPolygons() {
        image = CommonFunctions.convertImage(image);
        ImagePlus imp = new ImagePlus("test", image);
        ManyBlobs allBlobs = new ManyBlobs(imp); // Extended ArrayList
        allBlobs.findConnectedComponents(); // Start the Connected Component
        allBlobs = allBlobs.filterBlobs(500, 1000000000, Blob.GETENCLOSEDAREA);
//		System.out.println("AllBlobs size: " + allBlobs.size());
        if (allBlobs.size() != 2) {
            return false;
        }
        pNew1 = CommonFunctions.adjustPolygon(allBlobs.get(0).getOuterContour());
        pNew2 = CommonFunctions.adjustPolygon(allBlobs.get(1).getOuterContour());
        pNew1 = DrawGT.adjustPolygon(pNew1);
        pNew2 = DrawGT.adjustPolygon(pNew2);
        if (pNew1.getBounds().y < pNew2.getBounds().y) {
            pNew1.translate(0, 4);
            pNew2.translate(0, -4);
            DrawGT.separateTwoPolygons(pNew1, pNew2);
        } else {
            pNew1.translate(0, -4);
            pNew2.translate(0, 4);
            DrawGT.separateTwoPolygons(pNew2, pNew1);
        }
        polygonsGT.add(pNew1);
        polygonsGT.add(pNew2);

        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.black);
        g2d.fillPolygon(pNew1);
        g2d.fillPolygon(pNew2);
//		System.out.println("generate2NewPolygons is finished.");
        return true;
    }

    /**
     * Save the result to a HashMap.
     * 
     * @param myPoints
     * @param xSplit
     * @param ySplit
     * @return
     */
    public static Map<String, List<int[][]>> getResults(MyPoint[] myPoints, int xSplit, int ySplit) {
        polygonsGT.clear();
        results.clear();
        generatePolygonImage(myPoints);
        splitPolygon(xSplit, ySplit);
//		generate2NewPolygons();
        List<int[][]> splitPolygonsList = new ArrayList<int[][]>();
        for (Polygon polygon : polygonsGT) {
            polygon.translate(polygonBound.x, polygonBound.y);
            int[][] points = new int[polygon.npoints][2];
            for (int i = 0; i < polygon.npoints; i++) {
                points[i][0] = polygon.xpoints[i];
                points[i][1] = polygon.ypoints[i];
            }
            splitPolygonsList.add(points);
        }
        results.put("textLines", splitPolygonsList);
        return results;
    }
}
