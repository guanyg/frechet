package me.yung.frechet.bb;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Field;

import java.math.BigInteger;

public class IPSPEPointAdapter {

    private final IPSPE ipspe;
    private final BigInteger p;

    public IPSPEPointAdapter(IPSPE ipspe) {
        this.ipspe = ipspe;
        this.p = ipspe.getPp().getP();
    }

    @SuppressWarnings({"rawtypes"})
    public Field getG() {
        return ipspe.pp.getE().getG1();
    }

    public Element[] encrypt(int[] x) {
        BigInteger sum = BigInteger.ZERO;
        for (int i : x) {
            sum = sum.add(BigInteger.valueOf((long) i * i)).mod(p);
        }
        sum = p.subtract(sum);
        BigInteger[] eV = new BigInteger[x.length + 2];
        eV[0] = sum;
        eV[1] = BigInteger.ONE;
        for (int i = 0; i < x.length; i++) {
            eV[2 + i] = BigInteger.valueOf(x[i] * 2L).mod(p);
        }

        return ipspe.encrypt(eV);
    }

    public Element[] tokenGen(int[] x, int epsilon) {
        BigInteger sum = BigInteger.ZERO;
        for (int intVal : x) {
            sum = sum.add(BigInteger.valueOf((long) intVal * intVal).mod(p)).mod(p);
        }
        sum = BigInteger.valueOf((long) epsilon * epsilon).add(p).subtract(sum).mod(p);
        BigInteger[] eV = new BigInteger[x.length + 2];
        eV[0] = BigInteger.ONE;
        eV[1] = sum;
        for (int i = 0; i < x.length; i++) {
            eV[i + 2] = BigInteger.valueOf(x[i]);
        }

        return ipspe.tokenGen(eV);
    }

    public boolean check(Element[] dataVector, Element[] tokenVector) {
        return ipspe.check(dataVector, tokenVector);
    }
}
