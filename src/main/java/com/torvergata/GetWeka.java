package com.torvergata;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import weka.core.Instance;
import weka.core.Instances;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.bayes.NaiveBayes;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.lazy.IBk;

public class GetWeka {
    static Logger logger;

	private static final String[] classifiers = {"Random Forest", "IBk", "Naive Bayes"};
	private static final String[] features = {"No selection", "Best First"};
	private static final String[] samplings = {"No sampling", "Oversampling", "Undersampling", "Smote"};
	private static final String[] sensitives = {"No cost sensitive", "Sensitive threshold", "Sensitive learning"};

	
    static  String projectName ="TAJO";

    public static void main(String[] args) throws Exception {
		logger = Logger.getLogger(GetMetrics.class.getName());

		String projectName ="TAJO";

		List<Release> releases = GeJiraReleases.getReleases(projectName);
		int halfIndex = (int)((double)releases.size()/2 + 0.5);
        var halfVersions = releases.subList(0, halfIndex);

		createSets(halfVersions, projectName);
	}

	// with algo walk forward
	public static void createSets(List<Release> releases, String projectName) throws Exception {
		List<EvaluationResult> results = new ArrayList<>();

		ArrayList<Instances> trainList = new ArrayList<>();
		ArrayList<Instances> testList = new ArrayList<>();

		AbstractClassifier classifier = null;

		for (int i = 1; i < releases.size(); i++) {
			String pathTrain = projectName + "-dataset-" + i + ".arff";
			String pathTest = projectName + "-dataset-" + (i+1) + ".arff";
			DataSource source1 = new DataSource(pathTrain);
			Instances training = source1.getDataSet();
			DataSource source2 = new DataSource(pathTest);
			Instances testing = source2.getDataSet();

			int numAttr = training.numAttributes();
			// set attribute to predict: buggy (last attribute)
			training.setClassIndex(numAttr - 1);
			testing.setClassIndex(numAttr - 1);

			// union instances with previos instances
			for (int x = 0; x < (i-1); x++) {
				training.addAll(trainList.get(x));
			}

			trainList.add(training);
			testList.add(testing);


			for (int x = 0; x < classifiers.length; x++) {
				switch (classifiers[x]) {					
					case "Random Forest": 
						classifier = new RandomForest();			
					break;					
					case "IBk": 
						classifier = new IBk();				
					break;
					case "Naive Bayes": 
						classifier = new NaiveBayes();					
					break;					
					default:			
					break;
				}

				for (int f = 0; f < features.length; f++) {

					Filter filter = null;
					Instances filteredTrainSet = null;
					Instances filteredTestSet = null;
	
					switch (features[f]) {					
						case "No selection":			
						break;					
						case "Best First": 
							filter = cfs(training);						
						break;					
						default:			
						break;
					}

					if (filter != null) {
				
						try {
							filteredTrainSet = Filter.useFilter(training, filter);
							filteredTestSet = Filter.useFilter(training, filter);				
						} catch (Exception e) {
							logger.info(e.toString());
							return;
						}
						
						filteredTrainSet.setClassIndex(filteredTrainSet.numAttributes() - 1);
						filteredTestSet.setClassIndex(filteredTestSet.numAttributes() - 1);

						// balancing
						chooseSampling(results, classifier, filteredTrainSet, filteredTestSet, features[f], classifiers[x], i);
					}
					else
					{
						// wthout feture selection
						chooseSampling(results, classifier, training, testing, features[f], classifiers[x], i);
					}			
				}
			}
		}
		
		// add walkForward as trainingRelease with means

		List<EvaluationResult> resultsWithMean = getMeanInstances(results, projectName);

		writeWekaResultsCSV(resultsWithMean, projectName);
		logger.info("Finish machine learning");

	}

	private static List<EvaluationResult> getMeanInstances(List<EvaluationResult> results, String projectName) {

		// 72 = 3*2*4*3 combinations -> means
		List<EvaluationResult> newResults = new ArrayList<>();

		for(String classifier : classifiers) {
			for(String feature : features) {
				for(String sampling : samplings) {
					for(String cost : sensitives) {
						List<EvaluationResult> tempResults = new ArrayList<>();
						for (EvaluationResult evaluationResult : results) {
							if (evaluationResult.getClassifier().equals(classifier)
							&& evaluationResult.getSelection().equals(feature)
							&& evaluationResult.getBalancing().equals(sampling)
							&& evaluationResult.getCostSensitive().equals(cost)) {
								tempResults.add(evaluationResult);
								logger.info("Version: " + evaluationResult.getTrainingRelease() + " " +
								Integer.toString(evaluationResult.getIndex()));
							}
						}
						EvaluationResult meanResult = new EvaluationResult(projectName, "output-mean");
						meanResult.setClassifier(classifier);
						meanResult.setSelection(feature);
						meanResult.setBalancing(sampling);
						meanResult.setCostSensitive(cost);
						calculateMean(meanResult, tempResults);
						tempResults.add(meanResult);
						newResults.addAll(tempResults);
					}			
				}
			}
		}

		return newResults;
	}

	private static void calculateMean(EvaluationResult result, List<EvaluationResult> results) {
		 double percDataTraining = 0;
		 double percBugInTraining = 0;
		 double percBugInTesting = 0;
		 double tp = 0;
		 double fp = 0;
		 double tn = 0;
		 double fn = 0;
		 double precision = 0;
		 double recall = 0;
		 double auc = 0;
		 double kappa = 0;

		 int count = 0;

		for (EvaluationResult evaluationResult : results) {
			count++;
			percDataTraining+=evaluationResult.getPercDataTraining();
			percBugInTraining+=evaluationResult.getPercBugInTraining();
			percBugInTesting+=evaluationResult.getPercBugInTesting();
			tp+=evaluationResult.getTp();
			fp+=evaluationResult.getFp();
			tn+=evaluationResult.getTn();
			fn+=evaluationResult.getFn();
			precision+=evaluationResult.getPrecision();
			recall+=evaluationResult.getRecall();
			auc+=evaluationResult.getAuc();
			kappa+=evaluationResult.getKappa();
		}
		if (count > 0) {
			Double perc = percDataTraining/count;
			result.setPercDataTraining(Math.round(perc*100)/100.0d);
			perc = percBugInTraining/count;
			result.setPercBugInTraining(Math.round(perc*100)/100.0d);
			perc = percBugInTesting/count;
			result.setPercBugInTesting(Math.round(perc*100)/100.0d);
			result.setTp(tp/count);
			result.setFp(fp/count);
			result.setTn(tn/count);
			result.setFn(fn/count);
			result.setPrecision(precision/count);
			result.setRecall(recall/count);
			result.setAuc(auc/count);
			result.setKappa(kappa/count);
		}

	}

	private static void calculate(EvaluationResult newResult, Evaluation eval) {

		if(newResult.getBalancing() != null && !newResult.getBalancing().equals("No sampling")) {
			
			newResult.setPercBugInTraining(50);
			newResult.setPercBugInTesting(50);
		}
		else {
			Double perc = ((double) newResult.getBugInTrain() / (double)newResult.getTrainSize())*100;
			newResult.setPercBugInTraining(Math.round(perc*100)/100.0d);
			perc = ((double) newResult.getBugInTest() / (double) newResult.getTestSize())*100;
			newResult.setPercBugInTesting(Math.round(perc*100)/100.0d);
		}

		newResult.setTp(eval.numTruePositives(0));
		newResult.setFp(eval.numFalsePositives(0));
		newResult.setTn(eval.numTrueNegatives(0));
		newResult.setFn(eval.numFalseNegatives(0));

		newResult.setPrecision(eval.precision(0));
		newResult.setRecall(eval.recall(0));
		newResult.setAuc(eval.areaUnderROC(0));
		newResult.setKappa(eval.kappa());
		logger.info(Integer.toString(newResult.getIndex()));
		logger.info(Double.toString(eval.kappa()));

	}

	private static void chooseSampling(List<EvaluationResult> evals, AbstractClassifier classifier, Instances trainSet, Instances testSet, String featureName, String classifierName, int version) {

		for (int s = 0; s < samplings.length; s++) {
			FilteredClassifier filteredClassifier = null;
			Evaluation eval = null;
			Instances filteredTrainSet = null;

			switch (samplings[s]) {					
				case "No sampling":		
				break;					
				case "Oversampling": 
					Resample resample = oversampling(trainSet);
						
					filteredClassifier = new FilteredClassifier();
					filteredClassifier.setClassifier(classifier);
					filteredClassifier.setFilter(resample);
					
					try
					{
						filteredTrainSet = Filter.useFilter(trainSet, resample);
					}					
					catch (Exception e) {
						logger.info(e.toString());
					}			
				break;
				case "Undersampling": 
					SpreadSubsample spreadSubsample = undersampling(trainSet);
						
					filteredClassifier = new FilteredClassifier();
					filteredClassifier.setClassifier(classifier);
					filteredClassifier.setFilter(spreadSubsample);
					try
					{
						filteredTrainSet = Filter.useFilter(trainSet, spreadSubsample);
					}					
					catch (Exception e) {
						logger.info(e.toString());
					}	
				break;
				case "Smote": 					
					filteredTrainSet = smote(classifier, trainSet);
					try {
						if (classifier != null) {
							classifier.buildClassifier(filteredTrainSet);
							eval = new Evaluation(testSet);	
							eval.evaluateModel(classifier, testSet);
						}		
					} catch (Exception e) {
						logger.info(e.toString());
					}
						
				break;					
				default:				
				break;
			}

			if (filteredClassifier != null) {
				try {
					filteredClassifier.buildClassifier(filteredTrainSet);
					eval = new Evaluation(testSet);	
					eval.evaluateModel(classifier, testSet);
				} catch (Exception e) {
					logger.info(e.toString());
				}
				
			}

			if (classifier != null && (eval == null || filteredTrainSet == null)) {
				try {
					filteredTrainSet = trainSet;
					classifier.buildClassifier(filteredTrainSet);
					eval = new Evaluation(testSet);	
					eval.evaluateModel(classifier, testSet);
				} catch (Exception e) {
					logger.info(e.toString());
				}
			}

			int numAttr = filteredTrainSet.numAttributes();
			EvaluationResult newResult = new EvaluationResult(projectName, Integer.toString(version));
			Double perc = ((double)filteredTrainSet.size() / (filteredTrainSet.size()+testSet.size()))*100;
			newResult.setPercDataTraining(Math.round(perc*100)/100.0d);
			newResult.setClassifier(classifierName);
			newResult.setSelection(featureName);
			newResult.setBalancing(samplings[s]);
			newResult.setCostSensitive(sensitives[0]);
			newResult.setIndex(evals.size());

			// get buggy
			int bugInTrain = 0;
			for(Instance instance: filteredTrainSet){
				bugInTrain += (int)instance.value(numAttr - 1);
			}

			int bugInTest = 0;
			for(Instance instance: testSet){
				bugInTest += (int)instance.value(numAttr - 1);
			}
			newResult.setBugInTest(bugInTest);
			newResult.setBugInTrain(bugInTrain);
			newResult.setTrainSize(filteredTrainSet.size());
			newResult.setTestSize(testSet.size());
			if (eval != null) {
				calculate(newResult, eval);
			}
			evals.add(newResult);

			chooseSensitive(evals, classifier, filteredClassifier, filteredTrainSet, testSet, featureName, classifierName, samplings[s], version);
		}
	}

	private static void chooseSensitive(List<EvaluationResult> evals, AbstractClassifier classifier, FilteredClassifier filteredClassifier, Instances trainSet, Instances testSet, String featureName, String classifierName, String samplingsName, int version)
	{
		for (int s1 = 0; s1 < sensitives.length; s1++) {
			Evaluation eval = null;

			CostSensitiveClassifier costSensitiveClassifier = null;

			switch (sensitives[s1]) {					
				case "No cost sensitive":			
				break;
				case "Sensitive threshold": 
					costSensitiveClassifier = new CostSensitiveClassifier();

					if (filteredClassifier == null){
						costSensitiveClassifier.setClassifier(classifier);
					}
					else costSensitiveClassifier.setClassifier(filteredClassifier);

					costSensitiveClassifier.setCostMatrix(createCostMatrix(1, 10));
					costSensitiveClassifier.setMinimizeExpectedCost(false);
					try {
						costSensitiveClassifier.buildClassifier(trainSet);
						eval = new Evaluation(testSet);	
						eval.evaluateModel(costSensitiveClassifier, testSet);
					} catch (Exception e) {
						logger.info(e.toString());
					}
				break;
				case "Sensitive learning": 
					costSensitiveClassifier = new CostSensitiveClassifier();
					if (filteredClassifier==null){
						costSensitiveClassifier.setClassifier(classifier);
					}
					else costSensitiveClassifier.setClassifier(filteredClassifier);

					costSensitiveClassifier.setCostMatrix(createCostMatrix(1, 10));
					costSensitiveClassifier.setMinimizeExpectedCost(true);
					try {
						costSensitiveClassifier.buildClassifier(trainSet);
						eval = new Evaluation(testSet);	
						eval.evaluateModel(costSensitiveClassifier, testSet);
					} catch (Exception e) {
						logger.info(e.toString());
					}
				break;
				default:				
				break;
			}
			if (eval == null) {
				continue;
			}

			int numAttr = trainSet.numAttributes();

			EvaluationResult newResult = new EvaluationResult(projectName, Integer.toString(version));
			Double perc = (double)trainSet.size() / (trainSet.size()+testSet.size())*100;
			newResult.setPercDataTraining(Math.round(perc*100)/100.0d);
			newResult.setClassifier(classifierName);
			newResult.setSelection(featureName);
			newResult.setBalancing(samplingsName);
			newResult.setCostSensitive(sensitives[s1]);
			newResult.setIndex(evals.size());

			// get buggy
			int bugInTrain = 0;
			for(Instance instance: trainSet){
				bugInTrain += (int)instance.value(numAttr - 1);
			}

			int bugInTest = 0;
			for(Instance instance: testSet){
				bugInTest += (int)instance.value(numAttr - 1);
			}
			newResult.setBugInTest(bugInTest);
			newResult.setBugInTrain(bugInTrain);
			newResult.setTrainSize(trainSet.size());
			newResult.setTestSize(testSet.size());
			calculate(newResult, eval);
			evals.add(newResult);
		}
	}

	private static CostMatrix createCostMatrix(double weightFalsePositive, double weightFalseNegative) {
	    CostMatrix costMatrix = new CostMatrix(2);
	    costMatrix.setCell(0, 0, 0.0);
	    costMatrix.setCell(1, 0, weightFalsePositive);
	    costMatrix.setCell(0, 1, weightFalseNegative);
	    costMatrix.setCell(1, 1, 0.0);
	    return costMatrix;
	}

	private static Resample oversampling(Instances trainingSet) throws SecurityException {
		Resample resample = null;
		int bugsNumber = 0;
		try {
			resample = new Resample();
			resample.setInputFormat(trainingSet);			
			resample.setNoReplacement(false);
			resample.setBiasToUniformClass(1.0f);
			
			//get number defect
			for (var i = 0; i < trainingSet.size(); i++) {
				if (trainingSet.get(i).value(trainingSet.numAttributes() - 1) == 1)
				{
					bugsNumber++;
				}
			}
			
			double percentMinority = getMinorityPercent(trainingSet.size(), bugsNumber);
			resample.setSampleSizePercent((100 - percentMinority)*2);			
		} 
		catch (Exception e) {
			return null;
		}
		
		return resample;
	}

	private static SpreadSubsample undersampling(Instances trainingSet) throws SecurityException {
		SpreadSubsample spreadSubsample = null;
		var options = new String[]{"-M", "1.0"};

		try {
			
			spreadSubsample = new SpreadSubsample();
			spreadSubsample.setInputFormat(trainingSet);	
			spreadSubsample.setOptions(options);
			
		} 
		catch (Exception e) { 
			return null;
		}
		
		return spreadSubsample;	
	}

	private static Instances smote(AbstractClassifier classifier, Instances trainingSet) throws SecurityException {

		var smote = new SMOTE();
		try {
			smote.setInputFormat(trainingSet);
		}
		catch(Exception e)
		{
			logger.info(e.toString());
		}
		
		var filteredClassifier = new FilteredClassifier();
		filteredClassifier.setClassifier(classifier);
		filteredClassifier.setFilter(smote);
		
		try {
			trainingSet = Filter.useFilter(trainingSet, smote);
		} 
		catch (Exception e) { 
			logger.info(e.toString());
			return null;
		}
		
		return trainingSet;
	}

	private static double getMinorityPercent(int size, int bugs) {
		int minority = bugs;
		if ((size - bugs) < bugs)
			minority = size-bugs;
		
		return (double) minority/size * 100;
	}

	private static Filter cfs(Instances trainSet) throws SecurityException {
		
		AttributeSelection filter = new AttributeSelection();		
		CfsSubsetEval eval = new CfsSubsetEval();

		try 
		{ 
			eval.buildEvaluator(trainSet);
			
		    BestFirst bestFirst = new BestFirst();
		    
			filter.setEvaluator(eval);
			filter.setSearch(bestFirst);
			
			filter.setInputFormat(trainSet); 
		} 
		catch (Exception e) { 
			logger.info(e.toString());	
		}				
		return filter;
	}

	private static void writeWekaResultsCSV(List<EvaluationResult> results, String projName) {
        String outname = projName + "-model.csv";
		try(FileWriter fileWriter = new FileWriter(outname)) {
			//Name of CSV for output
			fileWriter.append("Dataset,TrainingRelease,%DataTraining,%BugInTraining,%BugInTesting,"
			+ "Classifier,Balancing,Selection,CostSensitive,TP,FP,TN,FN,Precision,Recall,AUC,Kappa");
			fileWriter.append("\n");

			for (int i = 0; i < results.size(); i++) {
				fileWriter.append(results.get(i).getDataset());
				fileWriter.append(",");
                fileWriter.append(results.get(i).getTrainingRelease());
				fileWriter.append(",");
				fileWriter.append(Double.toString(results.get(i).getPercDataTraining()));
				fileWriter.append(",");
				fileWriter.append(Double.toString(results.get(i).getPercBugInTraining()));
				fileWriter.append(",");
				fileWriter.append(Double.toString(results.get(i).getPercBugInTesting()));
				fileWriter.append(",");
				fileWriter.append(results.get(i).getClassifier());
				fileWriter.append(",");
				fileWriter.append(results.get(i).getBalancing());
				fileWriter.append(",");
				fileWriter.append(results.get(i).getSelection());
				fileWriter.append(",");
                fileWriter.append(results.get(i).getCostSensitive());
				fileWriter.append(",");
				fileWriter.append(Double.toString(results.get(i).getTp()));
				fileWriter.append(",");
                fileWriter.append(Double.toString(results.get(i).getFp()));
				fileWriter.append(",");
				fileWriter.append(Double.toString(results.get(i).getTn()));
				fileWriter.append(",");
                fileWriter.append(Double.toString(results.get(i).getFn()));
				fileWriter.append(",");
				fileWriter.append(Double.toString(results.get(i).getPrecision()));
				fileWriter.append(",");
                fileWriter.append(Double.toString(results.get(i).getRecall()));
				fileWriter.append(",");
				fileWriter.append(Double.toString(results.get(i).getAuc()));
				fileWriter.append(",");
                fileWriter.append(Double.toString(results.get(i).getKappa()));
				fileWriter.append(",");
				fileWriter.append("\n");
			}

			fileWriter.flush();

		} catch (Exception e) {
			logger.log(java.util.logging.Level.SEVERE, "Error in csv writer.");
		}
    }
}
