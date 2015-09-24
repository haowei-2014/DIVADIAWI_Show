package ch.unifr.diuf.diva.gabor;

import ch.unifr.diuf.diva.gabor.modification.ErasePolygon;
import ch.unifr.diuf.diva.gabor.modification.MergePolygons;
import ch.unifr.diuf.diva.gabor.modification.SplitPolygon;
import com.google.gson.Gson;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;


public class GaborTextLineSegmentation {

    /**
     * Execute GaborTextLineSegmentation from the command line
     * @param args
     *             0: execution type
     *             1; path to the image
     *             2: image name
     *             3: rootFolder
     *             4: matlabInstallationFolder
     *             5: top
     *             6: bottom
     *             7: left
     *             8: right
     *             9: linkingRectWidth
     *             10: linkingRectHeight
     */
    public static void main(String args[]){
        switch(args[0]) {
            case "create":
                create(args);
                break;
            case "merge":
                merge(args);
                break;
            case "split":
                split(args);
                break;
            case "delete":
                delete(args);
                break;
        }
    }

    private static void delete(String[] args) {
        Gson gson = new Gson();
        MyPoint[] points = gson.fromJson(args[1], MyPoint[].class);
        Map<String, List<int[][]>> resultsErase = ErasePolygon.getResults(points, Integer.valueOf(args[2]), Integer.valueOf(args[3]));
        System.out.println(DivaServicesJsonHelper.toJson(resultsErase));
    }


    private static void split(String[] args) {
        Gson gson = new Gson();
        MyPoint[] points = gson.fromJson(args[1], MyPoint[].class);
        Map<String, List<int[][]>> resultsSplit = SplitPolygon.getResults(points, Integer.valueOf(args[2]), Integer.valueOf(args[3]));
        System.out.println(DivaServicesJsonHelper.toJson(resultsSplit));
    }

    private static void merge(String[] args) {
        String polygon1 = args[1];
        String polygon2 = args[2];

        Gson gson = new Gson();
        MyPoint[] points1 = gson.fromJson(polygon1, MyPoint[].class);
        MyPoint[] points2 = gson.fromJson(polygon2, MyPoint[].class);

        Map<String, List<int[][]>> resultMerge = MergePolygons.getResults(points1, points2);
        System.out.println(DivaServicesJsonHelper.toJson(resultMerge));
    }

    private static void create(String[] args) {
        try {
            BufferedImage bufferedImage = ImageIO.read(new File(args[1]));
            Info info = new Info(bufferedImage,
                    args[2],
                    args[3] + File.separator + "tmpData" + File.separator,
                    args[4],
                    args[3],
                    Integer.valueOf(args[5]),
                    Integer.valueOf(args[6]),
                    Integer.valueOf(args[7]),
                    Integer.valueOf(args[8]),
                    Integer.valueOf(args[9]),
                    Integer.valueOf(args[10]));
            Map<String, List<int[][]>> result = AutoSegment.start(info);
            String resultsJson = DivaServicesJsonHelper.toJson(result);
            //THIS WILL BE REPLACE BY SOMETHING BETTER IN THE FUTURE!
            System.out.println(resultsJson);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
