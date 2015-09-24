package ch.unifr.diuf.diva.gabor;

import ij.ImagePlus;
import ij.blob.Blob;
import ij.blob.ManyBlobs;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 *  Link CCs within the same line together by small rectangles. Draw contour obtained by our algorithm 
 *  and ground truth. 
 * @author WeiH
 *
 */

/**
 * @author hao
 */
public class LinkCCsV2 {
    public String pathName;
    public String originalName = null;
    public int width = 0;
    public static int linkingRectHeight = 0;
    public static int linkingRectWidth = 0;

    public enum Orientation {LEFT, RIGHT}
    // inner class including blobs within the same line


    public LinkCCsV2(String pathName, String originalName, Info info) {
        this.pathName = pathName;
        this.originalName = originalName;
        LinkCCsV2.linkingRectWidth = info.linkingRectWidth;
        LinkCCsV2.linkingRectHeight = info.linkingRectHeight;
    }


    /**
     * This function is to search the blobs leftwards and rightwards within the same line
     *
     * @param img
     */
    public void start(BufferedImage img, Info info) {
        width = img.getWidth();
        img = CommonFunctions.convertImage(img);
        ImagePlus imp = new ImagePlus("test", img);
        ManyBlobs allBlobs = new ManyBlobs(imp); // Extended ArrayList
        allBlobs.findConnectedComponents(); // Start the Connected Component
        allBlobs = allBlobs.filterBlobs(200, 100000, Blob.GETENCLOSEDAREA);
//		System.out.println("AllBlobs size: " + allBlobs.size());
        // save the polygon pairs that were linked.
        ArrayList<ArrayList<Blob>> linkedBlobPairs = new ArrayList<ArrayList<Blob>>();
        boolean alreadyLinked = false; // two blobs are already linked together.

        for (Blob blob : allBlobs) {
            alreadyLinked = false;
            // compute the left linking rectangle of the blob
            Rectangle leftRect = computeRect(blob, Orientation.LEFT);
            if (leftRect != null) {
                Polygon polygon = CommonFunctions.adjustPolygon(blob
                        .getOuterContour());
                // for all other blobs, check any one intersects with the left polygon. If it finds one, link them.
                for (Blob newBlob : allBlobs) {
                    if (newBlob != blob) {
                        Polygon polygonLeft = CommonFunctions.adjustPolygon(newBlob
                                .getOuterContour());
                        if (polygonLeft.intersects(leftRect)) {
                            if (linkedBlobPairs.size() == 0) {
                                img = CommonFunctions.drawRects(img, polygonLeft, polygon, leftRect);
                                ArrayList<Blob> linedBlobsTmp = new ArrayList<Blob>();
                                linedBlobsTmp.add(blob);
                                linedBlobsTmp.add(newBlob);
                                linkedBlobPairs.add(linedBlobsTmp);
                            } else {
                                // check the polygon pair was already linked.
                                for (ArrayList<Blob> linkedBlobs : linkedBlobPairs) {
                                    if ((blob == linkedBlobs.get(0) && newBlob == linkedBlobs.get(1)) ||
                                            (blob == linkedBlobs.get(1) && newBlob == linkedBlobs.get(0))) {
                                        alreadyLinked = true;
                                        break;
                                    }
                                }
                                if (!alreadyLinked) {
                                    img = CommonFunctions.drawRects(img, polygonLeft, polygon, leftRect);
                                    ArrayList<Blob> linedBlobsTmp = new ArrayList<Blob>();
                                    linedBlobsTmp.add(blob);
                                    linedBlobsTmp.add(newBlob);
                                    linkedBlobPairs.add(linedBlobsTmp);
                                }
                            }
                            break;
                        }
                    }
                }
            }

            alreadyLinked = false;
            Rectangle rightRect = computeRect(blob, Orientation.RIGHT);
            if (rightRect != null) {
                Polygon polygon = CommonFunctions.adjustPolygon(blob
                        .getOuterContour());
                for (Blob newBlob : allBlobs) {
                    if (newBlob != blob) {
                        Polygon polygonRight = CommonFunctions.adjustPolygon(newBlob
                                .getOuterContour());
                        if (polygonRight.intersects(rightRect)) {
                            if (linkedBlobPairs.size() == 0) {
                                img = CommonFunctions.drawRects(img, polygon, polygonRight, rightRect);
                                ArrayList<Blob> linedBlobsTmp = new ArrayList<Blob>();
                                linedBlobsTmp.add(blob);
                                linedBlobsTmp.add(newBlob);
                                linkedBlobPairs.add(linedBlobsTmp);
                            } else {
                                for (ArrayList<Blob> linkedBlobs : linkedBlobPairs) {
                                    if ((blob == linkedBlobs.get(0) && newBlob == linkedBlobs.get(1)) ||
                                            (blob == linkedBlobs.get(1) && newBlob == linkedBlobs.get(0))) {
                                        alreadyLinked = true;
                                        break;

                                    }
                                }
                                if (!alreadyLinked) {
                                    img = CommonFunctions.drawRects(img, polygon, polygonRight, rightRect);
                                    ArrayList<Blob> linedBlobsTmp = new ArrayList<Blob>();
                                    linedBlobsTmp.add(blob);
                                    linedBlobsTmp.add(newBlob);
                                    linkedBlobPairs.add(linedBlobsTmp);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
        try {
            File file = new File(pathName + info.prefix + "SegLinkCCsV2.png");
            if (file.exists()) {
                file.delete();
            }
            ImageIO.write(img, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
//		System.out.println("Linking is finished!");
    }

    /**
     * draw the linking rectangles
     *
     * @param img
     * @param linkingRects
     * @param lines
     */
    public void drawRects(BufferedImage img, java.util.List<Rectangle> linkingRects, java.util.List<line> lines) {
        ArrayList<RectangleClass> regularLineRects = new ArrayList<RectangleClass>();
        for (line l : lines) {
            if (l.regular) {
                regularLineRects.addAll(l.linkingRects);
            }
        }
        ArrayList<Rectangle> rectangles = new ArrayList<Rectangle>();
        for (RectangleClass r : regularLineRects) {
            rectangles.add(r.rectangle);
        }
        if (regularLineRects != null) {
            CommonFunctions.drawRects(img, rectangles, "SegLinkCCsRects");
            //		CommonFunctions.drawRects(pathName, img, regularLineRects, "SegLinkCCs.png");
        }
    }


    /**
     * compute the left or right linking rectangle of the CC
     *
     * @param blob
     * @param orientation
     * @return
     */
    public Rectangle computeRect(Blob blob, Orientation orientation) {
        Rectangle rect = new Rectangle();
        Rectangle boundingRect = CommonFunctions.adjustPolygon(
                blob.getOuterContour()).getBounds();
        rect.width = linkingRectWidth;
        rect.height = linkingRectHeight;
        rect.y = boundingRect.y + boundingRect.height / 2 - rect.height / 2;
        if (orientation == Orientation.LEFT) {
            rect.x = boundingRect.x + 10 - rect.width;
            if (rect.x < 0) {    // out of the left boundary
                return null;
            }
        } else {
            rect.x = boundingRect.x + boundingRect.width - 10;
            if (rect.x + rect.width >= width) {     // out of the right boundary
                return null;
            }
        }
        return rect;
    }

    /**
     * A class for rectangle.
     */
    public class RectangleClass {
        public Rectangle rectangle;  // rectangle itself
        public Polygon leftPolygon;  // its left polygon
        public Polygon rightPolygon;  // its right polygon

        RectangleClass(Rectangle rectangle, Polygon leftPolygon, Polygon rightPolygon) {
            this.rectangle = rectangle;
            this.leftPolygon = leftPolygon;
            this.rightPolygon = rightPolygon;
        }
    }

    public class line {
        public java.util.List<Blob> lineBlobs = new ArrayList<Blob>();
        //    	public ArrayList<Rectangle> linkingRects = new ArrayList<Rectangle>();
        public java.util.List<RectangleClass> linkingRects = new ArrayList<RectangleClass>();
        public Blob leftmostBlob;
        public Blob rightmostBlob;
        public Rectangle leftmostRect;
        public Rectangle rightmostRect;
        public int length;
        public boolean regular;

        line(Blob b) {
            lineBlobs.add(b);
            leftmostBlob = b;
            rightmostBlob = b;
            length = 0;
            regular = false;
        }

        public java.util.List<Polygon> getPolygons() {
            ArrayList<Polygon> polygons = new ArrayList<Polygon>();
            for (Blob b : lineBlobs) {
                polygons.add(b.getOuterContour());
            }
            return polygons;
        }
    }
}
