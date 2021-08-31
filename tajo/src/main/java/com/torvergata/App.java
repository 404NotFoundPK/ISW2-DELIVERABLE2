package com.torvergata;

import java.util.List;
import java.util.logging.Logger;

public class App 
{
    private static Logger logger;
    
    public static void main( String[] args ) throws Exception
    {
        logger = Logger.getLogger("App");

        // String projectName ="TAJO";
        String projectName ="BOOKKEEPER";

        GetMetrics.startGetMetrics(projectName);

		List<Release> releases = GeJiraReleases.getReleases(projectName);
		int halfIndex = (int)(releases.size()/2 + 0.5);
        var halfVersions = releases.subList(0, halfIndex);

		GetWeka.createSets(halfVersions, projectName);
        logger.info("Finished");
    }
}
