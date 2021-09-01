package com.torvergata;

public class FileMetrics {
    private String fileName;
    private int version;
    private int numberOfLines;
    private long age;
    private long weightAge;
    private int numberOfAuthors;
    private int numberOfCommits;
    private int numberOfBugs;
    private int changeSetSize;
    private int avgChangeSetSize;
    private int maxChangeSetSize;
    private int addedLines;
    private int avgAddedLines;
    private int maxAddedLines;
    private int changedLines;
    private int avgChangedLines;
    private int maxChangedLines;
    private int churn;
    private int avgChurn;
    private int maxChurn;
    private String buggy;

    public FileMetrics(String fileName) {
      this.fileName = fileName;
    }

    public String getFileName() {
      return this.fileName;
    }
  
    public void setFileName(String fileName) {
      this.fileName = fileName;
    }
 
    public int getVersion() {
      return this.version;
    }
 
    public void setVersion(int version) {
      this.version = version;
    }
 
    public int getNumberOfLines() {
      return this.numberOfLines;
    }
 
    public void setNumberOfLines(int numberOfLines) {
      this.numberOfLines = numberOfLines;
    }
 
    public long getAge() {
      return this.age;
    }
 
    public void setAge(long age) {
      this.age = age;
    }
 
    public long getWeightAge() {
      return this.weightAge;
    }
 
    public void seWeightAge(long weightAge) {
      this.weightAge = weightAge;
    }
 
    public int getNumberOfAuthors() {
      return this.numberOfAuthors;
    }
 
    public void setNumberOfAuthors(int numberOfAuthors) {
      this.numberOfAuthors = numberOfAuthors;
    }
 
    public int getNumberOfCommits() {
      return this.numberOfCommits;
    }
 
    public void setNumberOfCommits(int numberOfCommits) {
      this.numberOfCommits = numberOfCommits;
    }
  
    public int getChangeSetSize() {
      return this.changeSetSize;
    }
 
    public void setChangeSetSize(int changeSetSize) {
      this.changeSetSize = changeSetSize;
    }
 
    public int getAvgChangeSetSize() {
      return this.avgChangeSetSize;
    }
 
    public void setAvgChangeSetSize(int avgChangeSetSize) {
      this.avgChangeSetSize = avgChangeSetSize;
    }
 
    public int getMaxChangeSetSize() {
      return this.maxChangeSetSize;
    }
 
    public void setMaxChangeSetSize(int maxChangeSetSize) {
      this.maxChangeSetSize = maxChangeSetSize;
    }
 
    public int getAddedLines() {
      return this.addedLines;
    }
 
    public void setAddedLines(int addedLines) {
      this.addedLines = addedLines;
    }
 
    public int getAvgAddedLines() {
      return this.avgAddedLines;
    }
 
    public void setAvgAddedLines(int avgAddedLines) {
      this.avgAddedLines = avgAddedLines;
    }
 
    public int getMaxAddedLines() {
      return this.maxAddedLines;
    }
 
    public void setMaxAddedLines(int maxAddedLines) {
      this.maxAddedLines = maxAddedLines;
    }
 
    public int getChangedLines() {
      return this.changedLines;
    }
 
    public void setChangedLines(int changedLines) {
      this.changedLines = changedLines;
    }
 
    public int getAvgChangedLines() {
      return this.avgChangedLines;
    }
 
    public void setAvgChangedLines(int avgChangedLines) {
      this.avgChangedLines = avgChangedLines;
    }
 
    public int getMaxChangedLines() {
      return this.maxChangedLines;
    }
 
    public void setMaxChangedLines(int maxChangedLines) {
      this.maxChangedLines = maxChangedLines;
    }
 
    public int getChurn() {
      return this.churn;
    }
 
    public void setChurn(int churn) {
      this.churn = churn;
    }
 
    public int getAvgChurn() {
      return this.avgChurn;
    }
 
    public void setAvgChurn(int avgChurn) {
      this.avgChurn = avgChurn;
    }
 
    public int getMaxChurn() {
      return this.maxChurn;
    }
 
    public void setMaxChurn(int maxChurn) {
      this.maxChurn = maxChurn;
    }

    public int getNumberOfBugs() {
      return this.numberOfBugs;
    }

    public void setNumberOfBugs(int numberOfBugs) {
      this.numberOfBugs = numberOfBugs;
    }
 
    public String getBuggy() {
      return this.buggy;
    }
 
    public void setBuggy(String buggy) {
      this.buggy = buggy;
    }
  
}
