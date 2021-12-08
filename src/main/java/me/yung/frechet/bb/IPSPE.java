package me.yung.frechet.bb;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;
import org.jlinalg.IRingElement;
import org.jlinalg.Matrix;
import org.jlinalg.field_p.FieldPBigFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

@SuppressWarnings({"UnstableApiUsage", "rawtypes", "unchecked"})
public class IPSPE {
    private static final int NUM_THREAD = 4;

    final FieldPBigFactory fpFactory;
    final PublicParameter pp;
    private final Field field;
    private TokenKey tk;
    private EncryptionKey ek;

    public IPSPE(PublicParameter pp) {
        this(pp, new FieldPBigFactory(pp.getP()));
    }

    public IPSPE(PublicParameter pp, FieldPBigFactory fpFactory) {
        this.fpFactory = fpFactory;
        this.pp = pp;
        Matrix matrix = new Matrix<>(new BigInteger[][]{new BigInteger[]{BigInteger.ZERO}}, fpFactory);
        try {
            this.field = matrix.getEntries()[0][0].getClass().getDeclaredField("value");
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new UnsupportedOperationException("Incorrect version of jlinalg");
        }
    }

    public static IPSPE load(String location) {
        Path path = Paths.get(location);

        PublicParameter pp;
        try (FileInputStream fos1 = new FileInputStream(path.resolve("pp").toFile())) {
            pp = PublicParameter.load(fos1);
        } catch (FileNotFoundException e) {
            System.err.println("Generate secret keys");
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        assert pp != null;
        IPSPE ret = new IPSPE(pp);

        try (FileInputStream fos1 = new FileInputStream(path.resolve("ek").toFile())) {
            ret.ek = EncryptionKey.load(fos1, ret);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        try (FileInputStream fos1 = new FileInputStream(path.resolve("tk").toFile())) {
            ret.tk = TokenKey.load(fos1, ret);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return ret;
    }

    public static IPSPE keyGeneration(int kappa, int k, int T) {
        TypeACurveGenerator pg = new TypeACurveGenerator(160, kappa);
        PairingParameters pairingParameters = pg.generate();
//        PairingFactory.getInstance().setUsePBCWhenPossible(true);
        Pairing pairing = PairingFactory.getPairing(pairingParameters);
        BigInteger p = pairingParameters.getBigInteger("r");
        it.unisa.dia.gas.jpbc.Field<Element> Field_G1 = pairing.getG1();
        Element g = Field_G1.newRandomElement().getImmutable();
        Element h = Field_G1.newRandomElement().getImmutable();
        PublicParameter pp = new PublicParameter();
        pp.setE(pairing);

        Element egh = pairing.pairing(g, h).getImmutable();

        BloomFilter<byte[]> H;

        H = buildHSet(T, egh);

        pp.setH(H);
        pp.setP(p);
        pp.setG(g);
        pp.setT(T);
        pp.setPairingParameters(pairingParameters);

        IPSPE ipspe = new IPSPE(pp);
        FieldPBigFactory fpfactory = ipspe.fpFactory;

        // init matrices

        Matrix m1 = new Matrix<>(randomInvertible(6, p), fpfactory);
        Matrix m1i = m1.inverse();

        Matrix m2 = new Matrix<>(randomInvertible(k + 2, p), fpfactory);
        Matrix m2i = m2.inverse();

        BigInteger[] gamma1 = new BigInteger[k];
        for (int i = 0; i < k; i++) {
            gamma1[i] = randomZp(p);
        }

        BigInteger[] gamma2 = new BigInteger[k];
        for (int i = 0; i < k; i++) {
            gamma2[i] = randomZp(p);
        }

        TokenKey tk = new TokenKey();
        tk.setM1(m1);
        tk.setM2(m2);
        tk.setGamma1(gamma1);
        tk.setGamma2(gamma2);
        ipspe.setTk(tk);

        EncryptionKey ek = new EncryptionKey();
        ek.setM1(m1i);
        ek.setM2(m2i);
        ek.setH(h);
        ek.setGamma1(gamma1);
        ek.setGamma2(gamma2);
        ipspe.setEk(ek);

        return ipspe;
    }

    private static BloomFilter<byte[]> buildHSet(long t, Element egh) {
        BloomFilter<byte[]> ret = BloomFilter.create(Funnels.byteArrayFunnel(), t * t + t, 0.05);
//        Set<ByteBuffer> H = new HashSet<>();
        long step = (t * t + t) / NUM_THREAD;
        long[] sep = new long[NUM_THREAD + 1];
        for (int i = 0; i < NUM_THREAD; i++) {
            sep[i] = step * i + 1;
        }
        sep[NUM_THREAD] = t * t + t + 1;

        try {
//        try (ProgressBar progressBar = new ProgressBar("Build H set", t * t + t)) {
            CountDownLatch latch = new CountDownLatch(NUM_THREAD);
            for (int i = 0; i < NUM_THREAD; i++) {
                int finalI = i;
                new Thread(() -> {
//                    HashSet<byte[]> localResult = new HashSet<>(sep[finalI + 1] - sep[finalI]);
                    Element prev = null;
                    for (long j = sep[finalI]; j < sep[finalI + 1]; j++) {
                        if (prev != null) {
                            prev = prev.mul(egh);
                        } else {
                            prev = egh.pow(BigInteger.valueOf(j));
                        }
//                        T.out.println(j + ": " + prev);
//                        T.out.println("egh: " + egh);
                        ret.put(prev.toBytes());
//                        localResult.add(h(prev.toBytes()));
//                        progressBar.step();
                    }
//                    synchronized (ret) {
//                        localResult.forEach(ret::put);
//                    }
                    latch.countDown();
                }).start();
            }
            latch.await();

        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return ret;
    }

    private static BigInteger[][] randomInvertible(int d, BigInteger p) {
        BigInteger[] m1 = new BigInteger[d];
        for (int i = 0; i < d; i++) {
            m1[i] = randomZp(p);
        }

        BigInteger[][] m2 = new BigInteger[d][d];
        for (int i = 0; i < d; i++) {
            for (int j = 0; j < i + 1; j++) {
                m2[i][j] = randomZp(p).multiply(m1[j]).mod(p);
            }
        }

        BigInteger[][] m3 = new BigInteger[d][d];

        for (int i = 0; i < d; i++) {
            for (int j = 0; j < d; j++) {
                BigInteger v = BigInteger.ZERO;
                for (int k = 0; k < i + 1; k++) {
                    v = v.add(m2[i][k].multiply(randomZp(p))).mod(p);
                }
                m3[i][j] = v;
            }
        }

        return m3;
    }

//    private static byte[] h(byte[] source) {
//        try {
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            return digest.digest(source);
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }
//        return new byte[0];
//    }

    private static BigInteger randomZp(BigInteger p) {
        BigInteger r;
        Random rnd = new Random();
        do {
            r = new BigInteger(p.bitLength(), rnd);
        } while (r.compareTo(p) >= 0);
        return r;
    }

    public it.unisa.dia.gas.jpbc.Field getG() {
        return pp.getE().getG1();
    }

    public PublicParameter getPp() {
        return pp;
    }

    public Element[] encrypt(BigInteger[] dataVector) {
        BigInteger[] eV = new BigInteger[dataVector.length + 2];

        System.arraycopy(dataVector, 0, eV, 0, dataVector.length);
        BigInteger gamma1 = randomZp(pp.getP());
        eV[dataVector.length] = gamma1; // gamma_1
        eV[dataVector.length + 1] = BigInteger.ONE;

        Matrix uT = new Matrix(new BigInteger[][]{eV}, fpFactory).transpose();

        Matrix uP;
        if (eV.length == ek.getM1().getCols()) {
            uP = ek.getM1().multiply(uT);
        } else if (eV.length == ek.getM2().getCols()) {
            uP = ek.getM2().multiply(uT);
        } else {
            throw new UnsupportedOperationException("wrong size");
        }

        BigInteger[] uPT = getEntries(uP.transpose())[0];
        Element[] ret = new Element[uPT.length + 1];
        for (int i = 0; i < uPT.length; i++) {
            ret[i] = ek.getH().pow(uPT[i]).getImmutable();
        }
        ret[uPT.length] = pp.getE().pairing(pp.getG(), ek.getH()).pow(pp.getP().subtract(gamma1)).getImmutable();
        return ret;
    }

    public Element[] tokenGen(BigInteger[] tokenVector) {
        BigInteger[] eV = new BigInteger[tokenVector.length + 2];

        BigInteger gamma3 = randomZp(new BigInteger(String.valueOf(pp.getT() - 2))).add(BigInteger.valueOf(2));
        BigInteger gamma2 = randomZp(gamma3.subtract(BigInteger.ONE)).add(BigInteger.ONE);

        for (int i = 0; i < tokenVector.length; i++) {
            eV[i] = gamma3.multiply(tokenVector[i]).mod(pp.getP());
        }

        eV[tokenVector.length] = BigInteger.ONE;
        eV[tokenVector.length + 1] = gamma2; // gamma_1

        Matrix v = new Matrix(new BigInteger[][]{eV}, fpFactory);
        Matrix vP;
        if (eV.length == tk.getM1().getCols()) {
            vP = v.multiply(tk.getM1());
        } else if (eV.length == tk.getM2().getCols()) {
            vP = v.multiply(tk.getM2());
        } else {
            throw new UnsupportedOperationException("wrong size");
        }

        BigInteger[] vPT = getEntries(vP)[0];
        Element[] ret = new Element[vPT.length];
        for (int i = 0; i < vPT.length; i++) {
            ret[i] = pp.getG().pow(vPT[i]).getImmutable();
        }
        return ret;
    }

    /**
     * @param dataVector  data vector
     * @param tokenVector token vector
     * @return true if dataVector * tokenVector > 0
     */
    public boolean check(Element[] dataVector, Element[] tokenVector) {
        if (dataVector.length != tokenVector.length + 1)
            return false;

        it.unisa.dia.gas.jpbc.Field gt = pp.getE().getGT();
        Element sum = gt.newOneElement();
        for (int i = 0; i < tokenVector.length; i++) {
            sum.mul(pp.getE().pairing(tokenVector[i], dataVector[i]));
        }
        sum.mul(dataVector[tokenVector.length]);
        return pp.getH().mightContain(sum.toBytes());
    }


    public void serialize(String f) {
        Path path = Paths.get(f);
        if (!path.toFile().exists()) {
            path.toFile().mkdirs();
            System.err.println("CREATE FOLDER " + path.toAbsolutePath().toFile().getPath());
        }

        try (FileOutputStream fos1 = new FileOutputStream(path.resolve("pp").toFile())) {
            this.pp.serialize(fos1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (FileOutputStream fos1 = new FileOutputStream(path.resolve("ek").toFile())) {
            this.ek.serialize(fos1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (FileOutputStream fos1 = new FileOutputStream(path.resolve("tk").toFile())) {
            this.tk.serialize(fos1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public TokenKey getTk() {
        return tk;
    }

    public void setTk(TokenKey tk) {
        this.tk = tk;
    }

    public EncryptionKey getEk() {
        return ek;
    }

    public void setEk(EncryptionKey ek) {
        this.ek = ek;
    }

    private BigInteger[][] getEntries(Matrix matrix) {
        IRingElement[][] entries = matrix.getEntries();
        int d1 = entries.length;
        int d2;
        if (d1 == 0 || (d2 = entries[0].length) == 0) return new BigInteger[0][];

        BigInteger[][] entriesBN = new BigInteger[d1][d2];
        for (int i = 0; i < d1; i++) {
            for (int j = 0; j < d2; j++) {
                try {
                    entriesBN[i][j] = (BigInteger) field.get(entries[i][j].abs());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    throw new UnsupportedOperationException("Incorrect version of jlinalg");
                }
            }
        }
        return entriesBN;
    }
}
