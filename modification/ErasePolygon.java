package ch.unifr.diuf.diva.gabor.modification;

import ch.unifr.diuf.diva.gabor.CommonFunctions;
import ch.unifr.diuf.diva.gabor.MyPoint;
import ch.unifr.diuf.diva.gabor.DrawGT;
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
 * This class is used to remove the redundant part in the endpoint of a text line
 * 
 * @author hao
 *
 */
public class ErasePolygon {

    public static BufferedImage image;
    public static Rectangle polygonBound;
    public static Polygon pNew;
    public static List<Polygon> polygonsGT = new ArrayList<>();
    public static Map<String, List<int[][]>> results = new HashMap<>();

    protected ErasePolygon() {
    }

    /**
     * get the bound of the polygon, and write the polygon as an image to the disk
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
     * Set a threshold, and remove other polygons.
     * 
     * @param xErase
     * @param yErase
     */
    public static void erasePolygon(int xErase, int yErase) {
        xErase -= polygonBound.x;
        yErase -= polygonBound.y;
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.white);
        g2d.fillRect(xErase - 1, 0, 3, polygonBound.height);

        image = CommonFunctions.convertImage(image);
        ImagePlus imp = new ImagePlus("test", image);
        ManyBlobs allBlobs = new ManyBlobs(imp); // Extended ArrayList
        allBlobs.findConnectedComponents(); // Start the Connected Component
        allBlobs = allBlobs.filterBlobs(50, 1000000000, Blob.GETENCLOSEDAREA);
//		System.out.println("AllBlobs size: " + allBlobs.size());
        if (allBlobs.size() != 2) {
//			System.out.println("allBlobs.size is not equal to 2!");
        } else {
            if (allBlobs.get(0).getAreaConvexHull() > allBlobs.get(1).getAreaConvexHull()) {
                pNew = CommonFunctions.adjustPolygon(allBlobs.get(0).getOuterContour());
            } else {
                pNew = CommonFunctions.adjustPolygon(allBlobs.get(1).getOuterContour());
            }
            pNew = DrawGT.interpolatePolygon(pNew);
            polygonsGT.add(pNew);
        }
    }

    /**
     * Save the result to a HashMap
     * 
     * @param myPoints
     * @param xErase
     * @param yErase
     * @return
     */
    public static Map<String, List<int[][]>> getResults(MyPoint[] myPoints, int xErase, int yErase) {
        polygonsGT.clear();
        results.clear();
        generatePolygonImage(myPoints);
        erasePolygon(xErase, yErase);

        List<int[][]> erasePolygonsList = new ArrayList<int[][]>();
        for (Polygon polygon : polygonsGT) {
            polygon.translate(polygonBound.x, polygonBound.y);
            int[][] points = new int[polygon.npoints][2];
            for (int i = 0; i < polygon.npoints; i++) {
                points[i][0] = polygon.xpoints[i];
                points[i][1] = polygon.ypoints[i];
            }
            erasePolygonsList.add(points);
        }
        results.put("textLines", erasePolygonsList);
        return results;
    }
}
