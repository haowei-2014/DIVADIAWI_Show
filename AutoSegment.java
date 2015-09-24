package ch.unifr.diuf.diva.gabor;

import ch.unifr.diuf.diva.gabor.Step1Projection;
import ch.unifr.diuf.diva.gabor.Step2Gabor;

import java.util.List;
import java.util.Map;

/**
 * @author hao
 *         <p>
 *         This class incorporates Step1Projection and Step2Gabor. It is the whole process.
 */
public class AutoSegment {

    protected AutoSegment() {
    }

    public static Map<String, List<int[][]>> start(Info info) {
        Map<String, List<int[][]>> result;

        // text blocks extraction using projection method
        Step1Projection step1Projection = new Step1Projection();
        step1Projection.initImage(info);
        step1Projection.cropTextBlock(info.top, info.bottom, info.left, info.right);

//		System.out.println("Text blocks extraction is done.");
        Step2Gabor step2Gabor = new Step2Gabor();
        result = step2Gabor.getResults(info.left, info.top, info.linkingRectWidth, info.linkingRectHeight, info);

        return result;
    }
}
