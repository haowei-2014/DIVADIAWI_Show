package ch.unifr.diuf.diva.gabor;

import matlabcontrol.*;

import java.io.File;

public class TextLinesGaborMatlab {

    protected TextLinesGaborMatlab() {
    }

    /**
     * Configuration of Matlab.
     * 
     * @param gaborInput
     * @param gaborOutput
     * @param matlabLocation
     * @param rootFolder
     * @throws MatlabConnectionException
     * @throws MatlabInvocationException
     */
    public static void textLinesExtraction(String gaborInput, String gaborOutput, String matlabLocation, String rootFolder) throws MatlabConnectionException, MatlabInvocationException {
        // create proxy
        MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder()
                .setUsePreviouslyControlledSession(true)
                .setMatlabLocation(matlabLocation).setHidden(true)
                .build();
        MatlabProxyFactory factory = new MatlabProxyFactory(options);
        MatlabProxy proxy = factory.getProxy();
        // call user-defined function (must be on the path)
        proxy.eval("addpath('" + rootFolder + File.separator + "GaborFilters')");
        proxy.feval("cvGaborTextureSegmentRun", gaborInput, gaborOutput);
        proxy.eval("rmpath('" + rootFolder + File.separator + "GaborFilters')");
        // close connection
        proxy.exit();
    }
}
