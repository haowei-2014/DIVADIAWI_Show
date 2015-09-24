package ch.unifr.diuf.diva.gabor;

import java.awt.image.BufferedImage;

/**
 * @author hao
 *         Information about the processed image
 */
public class Info {

    public BufferedImage bufferedImage;
    public String imageName;
    public int top;
    public int bottom;
    public int left;
    public int right;
    public int linkingRectWidth;
    public int linkingRectHeight;
    public String filePath = null;
    public String prefix = null;
    public String gaborInput = null;
    public String gaborOutput = null;
    public String matlabFolder;
    public String rootFolder;
    public Info(BufferedImage bufferedImage, String imageName, String filePath,String matlabFolder, String rootFolder, int top, int bottom, int left, int right, int linkingRectWidth, int linkingRectHeight) {
        this.bufferedImage = bufferedImage;
        this.imageName = imageName;
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
        this.linkingRectWidth = linkingRectWidth;
        this.linkingRectHeight = linkingRectHeight;

        this.filePath = filePath;
        this.matlabFolder = matlabFolder;
        this.rootFolder = rootFolder;
        prefix = "_" + left + "_" + top + "_";
        gaborInput = prefix + "GaborInput.png";
        gaborOutput = gaborInput.replace("GaborInput", "GaborOutput");
    }


}
