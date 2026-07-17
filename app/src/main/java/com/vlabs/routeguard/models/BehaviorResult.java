package com.vlabs.routeguard.models;

import java.util.List;

public class BehaviorResult {
    private int riskScore;
    private BehaviorRiskLevel riskLevel;
    private List<BehaviorFlag> flags;
    private String summary;

    public BehaviorResult() {
    }

    public BehaviorResult(int riskScore, BehaviorRiskLevel riskLevel, List<BehaviorFlag> flags, String summary) {
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.flags = flags;
        this.summary = summary;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public BehaviorRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(BehaviorRiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<BehaviorFlag> getFlags() {
        return flags;
    }

    public void setFlags(List<BehaviorFlag> flags) {
        this.flags = flags;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
