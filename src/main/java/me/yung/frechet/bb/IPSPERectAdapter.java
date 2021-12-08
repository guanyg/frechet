package me.yung.frechet.bb;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;

import java.math.BigInteger;

public class IPSPERectAdapter {
    private final IPSPE ipspe;
    private final BigInteger p;
    private final BigInteger[] gamma;

    public IPSPERectAdapter(IPSPE ipspe) {
        this.ipspe = ipspe;
        this.p = ipspe.getPp().getP();
        this.gamma = new BigInteger[0];
    }

    public IPSPERectAdapter(IPSPE ipspe, BigInteger[] gamma) {
        this.ipspe = ipspe;
        this.p = ipspe.getPp().getP();
        this.gamma = gamma;
    }

    public Pairing getPairing() {
        return ipspe.pp.getE();
    }

    public Element[][] encrypt(int[] xMin, int[] xMax) {
        Element[][] ret = new Element[xMin.length][];

        for (int i = 0; i < xMin.length; i++) {
            BigInteger[] v = new BigInteger[4];
            BigInteger gamma_hat = gamma[i];
            v[0] = gamma_hat.add(p).subtract(BigInteger.valueOf((long) xMin[i] * xMax[i]).mod(p)).mod(p);
            v[1] = BigInteger.ONE;
            v[2] = BigInteger.valueOf(xMin[i]);
            v[3] = BigInteger.valueOf(xMax[i]);

            ret[i] = ipspe.encrypt(v);
        }

        return ret;
    }

    public Element[][] tokenGen(int[] xMin, int[] xMax) {
        Element[][] ret = new Element[xMin.length][];

        for (int i = 0; i < xMin.length; i++) {
            BigInteger[] v = new BigInteger[4];
            BigInteger gamma_hat = gamma[i];
            v[0] = BigInteger.ONE;
            v[1] = p.subtract(gamma_hat.add(BigInteger.valueOf((long) xMin[i] * xMax[i]).mod(p)));
            v[2] = BigInteger.valueOf(xMin[i]);
            v[3] = BigInteger.valueOf(xMax[i]);

            ret[i] = ipspe.tokenGen(v);
        }
        return ret;
    }

    public boolean check(Element[][] dataVector, Element[][] tokenVector) {
        for (int i = 0; i < dataVector.length; i++) {
            if (!ipspe.check(dataVector[i], tokenVector[i])) {
                return false;
            }
        }
        return true;
    }
}
