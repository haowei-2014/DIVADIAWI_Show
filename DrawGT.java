package ch.unifr.diuf.diva.gabor;

import ch.unifr.diuf.diva.gabor.CommonFunctions;
import ch.unifr.diuf.diva.gabor.Info;
import ch.unifr.diuf.diva.gabor.TextLineExtraction;
import ij.ImagePlus;
import ij.blob.Blob;
import ij.blob.ManyBlobs;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DrawGT {
    public String pathName;
    public String fileName;
    public String originalName = TextLineExtraction.originalName;

    public DrawGT(String pathName) {
        this.pathName = pathName;
    }

    /**
     * This function is: (1) expand the polygon, lift the upper boundary and
     * lower the bottom boundary. (2) pick up every a few points on the original
     * polygon boundary and construct a new polygon
     *
     * @param polygon
     * @return
     */
    public static Polygon adjustPolygon(Polygon polygon) {
        ArrayList<Point> points = new ArrayList<Point>();
        int numberPoints = polygon.npoints;
        for (int i = 0; i < numberPoints; i++) {
            points.add(new Point(polygon.xpoints[i], polygon.ypoints[i]));
        }

        // find the leftmost and rightmost x coordinates
        int minX = 1000000;
        int maxX = 0;
        for (int i = 0; i < numberPoints; i++) {
            if (points.get(i).x > maxX) {
                maxX = points.get(i).x;
            }
            if (points.get(i).x < minX) {
                minX = points.get(i).x;
            }
        }

        int expand = 3; // Within a line, lift the upper boundary and lower the bottom boundary
        for (int i = minX; i <= maxX; i++) {
            ArrayList<Point> commonXPoints = new ArrayList<Point>();
            for (int j = 0; j < points.size(); j++) {
                if (points.get(j).x == i) {
                    commonXPoints.add(points.get(j));
                }
            }
            int mean = 0;
            for (int j = 0; j < commonXPoints.size(); j++) {
                mean += commonXPoints.get(j).y;
            }
            mean /= commonXPoints.size();
            for (int j = 0; j < commonXPoints.size(); j++) {
                if (commonXPoints.get(j).y < mean) {
                    commonXPoints.get(j).y -= expand; // lift
                } else {
                    commonXPoints.get(j).y += expand; // lower
                }
            }
        }
        int interval = 20; // pick up points every interval points
        int newNumberPoints = (numberPoints - 1) / interval + 1; // number of boundary points on the new polygon
        int[] xNewPoints = new int[(numberPoints - 1) / interval + 1];
        int[] yNewPoints = new int[(numberPoints - 1) / interval + 1];
        int newIndex = 0;
        for (int i = 0; i < numberPoints; i++) {
            if (i % interval == 0) {
                xNewPoints[newIndex] = points.get(i).x;
                yNewPoints[newIndex] = points.get(i).y;
                newIndex++;
            }
        }
        return new Polygon(xNewPoints, yNewPoints, newNumberPoints);
    }

    /**
     * pick up points of the polygon every interval
     * 
     * @param polygon
     * @return
     */
    public static Polygon interpolatePolygon(Polygon polygon) {
        ArrayList<Point> points = new ArrayList<Point>();
        int numberPoints = polygon.npoints;
        for (int i = 0; i < numberPoints; i++) {
            points.add(new Point(polygon.xpoints[i], polygon.ypoints[i]));
        }

        int interval = 20;
        int newNumberPoints = (numberPoints - 1) / interval + 1; // number of boundary points on the new polygon
        int[] xNewPoints = new int[(numberPoints - 1) / interval + 1];
        int[] yNewPoints = new int[(numberPoints - 1) / interval + 1];
        int newIndex = 0;
        for (int i = 0; i < numberPoints; i++) {
            if (i % interval == 0) {
                xNewPoints[newIndex] = points.get(i).x;
                yNewPoints[newIndex] = points.get(i).y;
                newIndex++;
            }
        }
        return new Polygon(xNewPoints, yNewPoints, newNumberPoints);
    }

    
    /**
     * This method is to avoid 2 polygons (upper and bottom ones) that intersect. 
     * It lifts the intersecting vertexes of the upper polygon,
     * and lowers the intersecting vertexes of the bottom polygon.
     * 
     * @param polygonsGT
     */
    public static void separatePolygonsGT(List<Polygon> polygonsGT) {
        for (int i = 0; i < polygonsGT.size(); i++) {
            Polygon polygon1 = polygonsGT.get(i);
            for (int j = i + 1; j < polygonsGT.size(); j++) {
                Polygon polygon2 = polygonsGT.get(j);
                if (polygon1.getBounds().y < polygon2.getBounds().y) {
                    separateTwoPolygons(polygon1, polygon2);
                } else {
                    separateTwoPolygons(polygon2, polygon1);
                }
            }
        }
    }

    /**
     * This method is to do concrete separation between two polygons if they intersect.
     * 
     * @param polygon1
     * @param polygon2
     */
    public static void separateTwoPolygons(Polygon polygon1, Polygon polygon2) {
        int[] xpoints1 = polygon1.xpoints;
        int[] ypoints1 = polygon1.ypoints;
        int[] xpoints2 = polygon2.xpoints;
        int[] ypoints2 = polygon2.ypoints;
        boolean hasIntersection = true;
        int countIteration = 0;

        while (hasIntersection) {
            countIteration++;
            if (countIteration == 10) {
                return;
            }
            hasIntersection = false;
            Point p;
            for (int i = 0; i < polygon1.npoints; i++) {
                p = new Point(polygon1.xpoints[i], polygon1.ypoints[i]);
                if (polygon2.contains(p)) {
                    ypoints1[i] -= 2;  // lift the vertex
                    hasIntersection = true;
                }
            }
            polygon1 = new Polygon(xpoints1, ypoints1, polygon1.npoints);
            for (int i = 0; i < polygon2.npoints; i++) {
                p = new Point(polygon2.xpoints[i], polygon2.ypoints[i]);
                if (polygon1.contains(p)) {
                    ypoints2[i] += 2;  // lower the vertex
                    hasIntersection = true;
                }
            }
            polygon2 = new Polygon(xpoints2, ypoints2, polygon2.npoints);
        }
    }

    /**
     * This is the entry point of the class.
     * 
     * @param info
     * @return
     */
    public List<Polygon> start(Info info) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(this.pathName + info.prefix + "SegLinkCCsV2.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        img = CommonFunctions.convertImage(img);
        ImagePlus imp = new ImagePlus("test", img);
        ManyBlobs allBlobs = new ManyBlobs(imp); // Extended ArrayList
        allBlobs.findConnectedComponents(); // Start the Connected Component
        allBlobs = allBlobs.filterBlobs(300, 100000, Blob.GETENCLOSEDAREA);
//        System.out.println("AllBlobs size: " + allBlobs.size());

        BufferedImage original = null;
        try {
            original = ImageIO.read(new File(pathName + originalName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        int horizontalOffset = (original.getWidth() - img.getWidth()) / 2;
        int verticalOffset = (original.getHeight() - img.getHeight()) / 2;
        ArrayList<Polygon> polygonsGT = new ArrayList<Polygon>();

        for (int i = 0; i < allBlobs.size(); i++) {
            Polygon adjustedPolygon = CommonFunctions.adjustPolygon(allBlobs
                    .get(i).getOuterContour());
            adjustedPolygon.translate(horizontalOffset + 1, verticalOffset + 1); // -1 is the error of ijblob.
            adjustedPolygon = adjustPolygon(adjustedPolygon);
            polygonsGT.add(adjustedPolygon);
        }
        separatePolygonsGT(polygonsGT);
        CommonFunctions.drawBoundaries(pathName, original, null, polygonsGT, null, "segmentation1.png");
        return polygonsGT;
    }

}
