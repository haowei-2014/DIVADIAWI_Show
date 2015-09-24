package ch.unifr.diuf.diva.gabor;

import ch.unifr.diuf.diva.gabor.Info;
import ch.unifr.diuf.diva.gabor.TextLineExtraction;
import ch.unifr.diuf.diva.gabor.TextLinesGaborMatlab;
import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Step2Gabor {
    public Map<String, List<int[][]>> results;
    public List<Polygon> polygonsGT = new ArrayList<Polygon>();
    public int linkingRectWidth;
    public int linkingRectHeight;
    public String gaborOutput; // file name of the gabor output image

    /**
     * Obtain result, and save it to a HashMap.
     * 
     * @param offsetX
     * @param offsetY
     * @param linkingRectWidth
     * @param linkingRectHeight
     * @param info
     * @return
     */
    public Map<String, List<int[][]>> getResults(int offsetX, int offsetY,
                                                 int linkingRectWidth, int linkingRectHeight, Info info) {
        this.linkingRectHeight = linkingRectHeight;
        this.linkingRectWidth = linkingRectWidth;


        gaborOutput = info.gaborOutput;
        // use jar of Gabor filter
        /*		GaborClustering.start(Step1Projection.filePath + Step1Projection.gaborInput
				, Step1Projection.filePath + gaborOutput);*/

        // use matlab directly
        try {
            TextLinesGaborMatlab.textLinesExtraction(info.filePath + info.gaborInput, info.filePath + info.gaborOutput, info.matlabFolder, info.rootFolder);
        } catch (MatlabConnectionException | MatlabInvocationException e) {
            e.printStackTrace();
        }

        TextLineExtraction tle = new TextLineExtraction();
//		System.out.println("offsetX is: " + offsetX + ", offsetY is: " + offsetY);
        polygonsGT = tle.start(offsetX, offsetY, info);
        results = new HashMap<String, List<int[][]>>();
        List<int[][]> textLinesList = new ArrayList<int[][]>();

        for (Polygon polygon : polygonsGT) {
            int[][] points = new int[polygon.npoints][2];
            for (int i = 0; i < polygon.npoints; i++) {
                points[i][0] = polygon.xpoints[i];
                points[i][1] = polygon.ypoints[i];
            }
            textLinesList.add(points);
        }

        results.put("textLines", textLinesList);
        return results;
    }
}
