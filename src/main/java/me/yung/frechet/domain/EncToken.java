package me.yung.frechet.domain;

import it.unisa.dia.gas.jpbc.Element;

public class EncToken {
    private Element[][] tokenL, tokenU;
    private EncTrajectory encTraj;

    public EncToken(Element[][] tokenL, Element[][] tokenU, EncTrajectory encTraj) {
        this.tokenL = tokenL;
        this.tokenU = tokenU;
        this.encTraj = encTraj;
    }

    public Element[][] getTokenL() {
        return tokenL;
    }

    public void setTokenL(Element[][] tokenL) {
        this.tokenL = tokenL;
    }

    public Element[][] getTokenU() {
        return tokenU;
    }

    public void setTokenU(Element[][] tokenU) {
        this.tokenU = tokenU;
    }

    public EncTrajectory getEncTraj() {
        return encTraj;
    }

    public void setEncTraj(EncTrajectory encTraj) {
        this.encTraj = encTraj;
    }
}
