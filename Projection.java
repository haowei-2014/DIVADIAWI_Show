package ch.unifr.diuf.diva.gabor;

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
 * This class is to find the separating lines to separate the touched connected components.
 *
 * @author WeiH
 */

public class Projection {
    public String pathName = null;
    public String fileName = null;

    public Projection(String pathName) {
        this.pathName = pathName;
    }

    /**
     * This function is to plot the horizontal projection plot.
     *
     * @param img
     * @return
     */
    public int[] plot(BufferedImage img) {
        int[][] imageData = new int[img.getHeight()][img.getWidth()];
        for (int i = 0; i < img.getHeight(); i++) {
            for (int j = 0; j < img.getWidth(); j++) {
                imageData[i][j] = img.getRaster().getSample(j, i, 0);
            }
        }
        double[] xValues = new double[imageData.length];
        for (int i = 0; i < xValues.length; i++) {
            xValues[i] = i;
        }
        int[] projectionData = horizontalProjection(imageData);
//        System.out.println("Projection values are:");
//        System.out.println(Arrays.toString(projectionData));
        double[] projectionDataDouble = new double[projectionData.length];
        for (int i = 0; i < projectionDataDouble.length; i++) {
            projectionDataDouble[i] = projectionData[i];
        }
//        Plot plotArray = new Plot("Horizontal projection", "x", "y", xValues, projectionDataDouble);
//		plotArray.show();
        return projectionData;
    }

    /**
     * This function is to compute horizontal projection. Because in the image, white is 255 and
     * black is 0, we have to deduct the white part, in order to get the number of black pixels.
     * The returned array is the numbers of black pixels.
     *
     * @param imageData
     * @return projectionData
     */
    public int[] horizontalProjection(int[][] imageData) {
        int[] projectionData = new int[imageData.length];
        for (int i = 0; i < imageData.length; i++) {
            for (int j = 0; j < imageData[0].length; j++) {
                projectionData[i] += imageData[i][j];
            }
            projectionData[i] = (255 * imageData[0].length - projectionData[i]) / 255;
        }
        return projectionData;
    }

    /**
     * This function is to find the locations of separating lines.
     *
     * @param projectionData
     * @return
     */
    public List<Integer> findSeparator(int[] projectionData) {
        int mean = 0;
        List<Integer> peaks = new ArrayList<>();
        List<Integer> peakValues = new ArrayList<>();
        List<Integer> valleys = new ArrayList<>();
        List<Integer> valleyValues = new ArrayList<>();

        for (int i = 0; i < projectionData.length; i++) {
            mean += projectionData[i];
        }
        mean = mean / projectionData.length;
        mean = (int) (0.5 * mean);

        for (int i = 1; i < projectionData.length - 1; i++) {
            if (projectionData[i] >= projectionData[i - 1] && projectionData[i] >= projectionData[i + 1]
                    && projectionData[i] > mean) {
                peaks.add(i);
                peakValues.add(projectionData[i]);
            }
            if (projectionData[i] <= projectionData[i - 1] && projectionData[i] <= projectionData[i + 1]
                    && projectionData[i] < mean) {
                valleys.add(i);
                valleyValues.add(projectionData[i]);
            }
        }
//        System.out.println("The peak locations are: " + peaks.toString());

        removeNeighbour(peaks, peakValues, true);
        removeNeighbour(valleys, valleyValues, false);

//        System.out.println("The mean is: " + mean);
//        System.out.println("The peak locations are: " + peaks.toString());
//        System.out.println("The peak values are: " + peakValues.toString());
//        System.out.println("The valley locations are: " + valleys.toString());
//        System.out.println("The valley values are: " + valleyValues.toString());

        for (int i = 0; i < valleys.size(); i++) {
            if (valleys.get(i) < peaks.get(0) || valleys.get(i) > peaks.get(peaks.size() - 1)) {
                valleys.remove(i);
                valleyValues.remove(i);
            }
        }

//        System.out.println(valleys.size() + " valleys are found. They are:");
//        System.out.println(valleys.toString());
        return valleys;
    }

    /**
     * In a neighborhood, there should be only one maximal peak or minimal valley. Thus, non-maximal
     * peaks and non-minimal valleys should be removed.
     *
     * @param locations
     * @param values
     * @param findPeak
     */
    public void removeNeighbour(List<Integer> locations, List<Integer> values, boolean findPeak) {
        boolean clear = true;
        while (true) {
            clear = true;
            for (int i = 0; i < locations.size() - 1; i++) {
                if (locations.get(i + 1) - locations.get(i) < 20) {
                    if (findPeak) {
                        if (values.get(i + 1) > values.get(i)) {
                            locations.remove(i);
                            values.remove(i);
                        } else {
                            locations.remove(i + 1);
                            values.remove(i + 1);
                        }
                    } else {
                        if (values.get(i + 1) < values.get(i)) {
                            locations.remove(i);
                            values.remove(i);
                        } else {
                            locations.remove(i + 1);
                            values.remove(i + 1);
                        }
                    }
                    clear = false;
                    break;
                }
            }
            if (clear) {
                break;
            }
        }
    }

    /**
     * Add more points.
     * 
     * @param separatingPoints
     * @return
     */
    public List<Point> pad(List<Point> separatingPoints) {
        List<Point> padding = new ArrayList<Point>();
        Point pre = null;
        Point current = null;

        Map<Integer, List<Point>> lines = new LinkedHashMap<>();

        for (Point p : separatingPoints) {
            if (!lines.containsKey(p.y)) {
                lines.put(p.y, new ArrayList<Point>());
                lines.get(p.y).add(p);
            }

        }

        for (int i = 1; i < separatingPoints.size(); i++) {
            pre = separatingPoints.get(i - 1);
            current = separatingPoints.get(i);
            if (current.y != pre.y) {
                padding.add(new Point(pre.x + 1, pre.y));
                padding.add(new Point(pre.x + 2, pre.y));
                padding.add(new Point(pre.x + 3, pre.y));
                padding.add(new Point(pre.x + 4, pre.y));
                padding.add(new Point(pre.x + 5, pre.y));
            } else if (current.x != pre.x + 1) {
                padding.add(new Point(pre.x + 1, pre.y));
                padding.add(new Point(pre.x + 2, pre.y));
                padding.add(new Point(pre.x + 3, pre.y));
                padding.add(new Point(pre.x + 4, pre.y));
                padding.add(new Point(pre.x + 5, pre.y));
            }
        }
        separatingPoints.addAll(padding);
//        System.out.println(padding.size() + " points are added.");
        return separatingPoints;
    }


    /**
     * This function is to create the separated CCs. The points on the separating lines are returned.
     *
     * @param img
     * @param valleys
     * @return separatingPoints
     */
    public List<Point> showResult(BufferedImage img, List<Integer> valleys) {
        List<Point> separatingPoints = new ArrayList<>();
        WritableRaster raster = img.getRaster();
/*		for (Integer i : valleys) {
            for (int j = 0; j < img.getWidth(); j++){
				if (raster.getSample(j,i, 0) != 255){
					raster.setSample(j, i, 0, 255);
					separatingPoints.add(new Point(j,i));
				}
			}
		}*/

        for (Integer i : valleys) {
            for (int j = 0; j < img.getWidth(); j++) {
                for (int k = i - 2; k < i + 2; k++) {
                    if (raster.getSample(j, k, 0) != 255) {
                        raster.setSample(j, k, 0, 255);
                    }
                    separatingPoints.add(new Point(j, k));
                }
            }
        }

//		combineTinyCCs(img, separatingPoints);
//		pad(separatingPoints);

        try {
            File file = new File(pathName + fileName + "Result.png");
            ImageIO.write(img, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return separatingPoints;
    }

    /**
     * When the function "showResult" is called, separating lines are draw. However, it may
     * create some tiny CCs, so these CCs should be reunited with the neighboring big CC.
     *
     * @param img
     * @param separatingPoints
     */
    public void combineTinyCCs(BufferedImage img, List<Point> separatingPoints) {
        List<Point> correctedPoints = new ArrayList<>();
        Rectangle rect;
        ImagePlus imp = new ImagePlus("", img);
        ManyBlobs allBlobs = new ManyBlobs(imp);
        allBlobs.findConnectedComponents();

/*		BufferedImage bufferedImage = new BufferedImage(img.getWidth(), img.getHeight(),
                BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bufferedImage.createGraphics();
		g2d.drawImage(img, 0, 0, null);
		g2d.setColor(Color.red);
		
		for (int i = 0; i < allBlobs.size(); i++){
			Polygon adjustedPolygon = CommonFunctions.adjustPolygon(allBlobs.get(i).getOuterContour());
//			Polygon adjustedPolygon = allBlobs.get(i).getOuterContour();
			rect = adjustedPolygon.getBounds();
//			g2d.drawPolygon(adjustedPolygon);
			g2d.drawRect(rect.x, rect.y, rect.width, rect.height);
		}
		g2d.dispose();
	
		try {
			// Save as PNG
			File file = new File(pathName + fileName + "WithPolygons.png");
			ImageIO.write(bufferedImage, "png", file);
		} catch (IOException e) {
			e.printStackTrace();
		}*/

        WritableRaster raster = img.getRaster();
//        System.out.println("The perimeters are:");
        for (int i = 0; i < allBlobs.size(); i++) {
//            System.out.println(allBlobs.get(i).getPerimeter());
            if (allBlobs.get(i).getPerimeter() < 50) {
                Polygon adjustedPolygon = CommonFunctions.adjustPolygon(allBlobs.get(i).getOuterContour());
                rect = adjustedPolygon.getBounds();
                for (Point p : separatingPoints) {
                    if ((p.y == (rect.y - 1) || p.y == (rect.y + rect.height + 1)) && (Math.abs(p.x - rect.x) < 20)) {
                        correctedPoints.add(p);
                        raster.setSample(p.x, p.y, 0, 0);
                    }
                }
            }
        }
        separatingPoints.removeAll(correctedPoints);
    }

    public List<Point> start(String fileName) {
        this.fileName = fileName;
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(pathName + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        img = CommonFunctions.convertImage(img);
        int[] projectionData = plot(img);
        List<Integer> valleys = findSeparator(projectionData);
        List<Point> separatingPoints = showResult(img, valleys);
//        System.out.println("Done!");
        return separatingPoints;
    }
}