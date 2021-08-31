package com.torvergata;

public class EvaluationResult {
  // Dataset,TrainingRelease,%DataTraining,%BugInTraining,%BugInTesting,"
	// 		+ "Classifier,Balancing,Selection,CostSensitive,TP,FP,TN,FN,Precision,Recall,AUC,Kappa

  private String dataset;
  private String trainingRelease;
  private double percDataTraining;
  private double percBugInTraining;
  private double percBugInTesting;
  private String classifier;
  private String balancing;
  private String selection;
  private String costSensitive;
  private double tp;
  private double fp;
  private double tn;
  private double fn;
  private double precision;
  private double recall;
  private double auc;
  private double kappa;
  private int bugInTrain;
  private int bugInTest;
  private int trainSize;
  private int testSize;
  private int index;

  public int getIndex() {
    return this.index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public int getBugInTrain() {
    return this.bugInTrain;
  }

  public void setBugInTrain(int bugInTrain) {
    this.bugInTrain = bugInTrain;
  }

  public int getBugInTest() {
    return this.bugInTest;
  }

  public void setBugInTest(int bugInTest) {
    this.bugInTest = bugInTest;
  }

  public int getTrainSize() {
    return this.trainSize;
  }

  public void setTrainSize(int trainSize) {
    this.trainSize = trainSize;
  }

  public int getTestSize() {
    return this.testSize;
  }

  public void setTestSize(int testSize) {
    this.testSize = testSize;
  }

  public String getDataset() {
    return this.dataset;
  }

  public void setDataset(String dataset) {
    this.dataset = dataset;
  }

  public String getTrainingRelease() {
    return this.trainingRelease;
  }

  public void setTrainingRelease(String trainingRelease) {
    this.trainingRelease = trainingRelease;
  }

  public double getPercDataTraining() {
    return this.percDataTraining;
  }

  public void setPercDataTraining(double percDataTraining) {
    this.percDataTraining = percDataTraining;
  }

  public double getPercBugInTraining() {
    return this.percBugInTraining;
  }

  public void setPercBugInTraining(double percBugInTraining) {
    this.percBugInTraining = percBugInTraining;
  }

  public double getPercBugInTesting() {
    return this.percBugInTesting;
  }

  public void setPercBugInTesting(double percBugInTesting) {
    this.percBugInTesting = percBugInTesting;
  }

  public String getClassifier() {
    return this.classifier;
  }

  public void setClassifier(String classifier) {
    this.classifier = classifier;
  }

  public String getBalancing() {
    return this.balancing;
  }

  public void setBalancing(String balancing) {
    this.balancing = balancing;
  }

  public String getSelection() {
    return this.selection;
  }

  public void setSelection(String selection) {
    this.selection = selection;
  }

  public String getCostSensitive() {
    return this.costSensitive;
  }

  public void setCostSensitive(String costSensitive) {
    this.costSensitive = costSensitive;
  }

  public double getTp() {
    return this.tp;
  }

  public void setTp(double tp) {
    this.tp = tp;
  }

  public double getFp() {
    return this.fp;
  }

  public void setFp(double fp) {
    this.fp = fp;
  }

  public double getTn() {
    return this.tn;
  }

  public void setTn(double tn) {
    this.tn = tn;
  }

  public double getFn() {
    return this.fn;
  }

  public void setFn(double fn) {
    this.fn = fn;
  }

  public double getPrecision() {
    return this.precision;
  }

  public void setPrecision(double precision) {
    this.precision = precision;
  }

  public double getRecall() {
    return this.recall;
  }

  public void setRecall(double recall) {
    this.recall = recall;
  }

  public double getAuc() {
    return this.auc;
  }

  public void setAuc(double auc) {
    this.auc = auc;
  }

  public double getKappa() {
    return this.kappa;
  }

  public void setKappa(double kappa) {
    this.kappa = kappa;
  }

    public EvaluationResult(String dataset, String trainingRelease) {
      this.dataset = dataset;
      this.trainingRelease = trainingRelease;
    }
}