package ch.unifr.diuf.diva.gabor;

import ch.unifr.diuf.diva.gabor.Info;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Step1Projection {

    public Map<String, List<int[][]>> results;
    public BufferedImage image = null;
    public String filePath = null;
    public String gaborInput = null;
    public String prefix = null;

    /**
     * Initialization.
     * 
     * @param info
     */
    public void initImage(Info info) {
        filePath = info.filePath;
        gaborInput = info.gaborInput;
        prefix = info.prefix;

        image = info.bufferedImage;
        int w = image.getWidth();
        int h = image.getHeight();
//        System.out.println("Width is: " + w + ", Height is: " + h);
    }

	
/*	// use Kai's projection profile method to extract text blocks
    public static HashMap<String, List<int[][]>> getResults(){
		ArrayList<Block> blockList = BlockRetriever.start(image);
		results = new HashMap<String, List<int[][]>>();	
		List<int[][]> textBlocksList = new ArrayList<int[][]>();
		
		for (int i = 0; i < blockList.size(); i++){
			textBlocksList.add(new int[][] {
				{blockList.get(i).x, blockList.get(i).y}, 
				{blockList.get(i).x + blockList.get(i).width, blockList.get(i).y}, 
				{blockList.get(i).x + blockList.get(i).width, blockList.get(i).y + blockList.get(i).height},
				{blockList.get(i).x, blockList.get(i).y + blockList.get(i).height}});
		}

	//	textBlocksList.add(new int[][] {{100, 300}, {400, 665}, {1000, 900}});
		results.put("textBlocks", textBlocksList);	
		return results;
	}*/

    /**
     * Obtain a text block, given the parameters.
     * 
     * @param top
     * @param bottom
     * @param left
     * @param right
     */
    public void cropTextBlock(int top, int bottom, int left, int right) {
        BufferedImage textBlock = image.getSubimage(left, top, right - left, bottom - top);
        try {
            File outputfile = new File(filePath + gaborInput);
            if (!outputfile.getParentFile().exists()) {
                outputfile.getParentFile().mkdirs();
            }
            if (!outputfile.exists()) {
                outputfile.createNewFile();
            }
            ImageIO.write(textBlock, "png", outputfile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
