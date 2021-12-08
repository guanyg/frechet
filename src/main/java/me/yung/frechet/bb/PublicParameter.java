package me.yung.frechet.bb;

import com.google.common.hash.BloomFilter;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.math.BigInteger;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static me.yung.frechet.bb.ElemSerde.deserializeElem;
import static me.yung.frechet.bb.ElemSerde.serializeElem;

@SuppressWarnings("UnstableApiUsage")
public class PublicParameter {
    private BigInteger p;
    private Pairing e;
    private BloomFilter<byte[]> h;
    private Element g;
    private int t;
    private PairingParameters pairingParameters;

    public static PublicParameter load(InputStream is) {
        if (!(is instanceof BufferedInputStream))
            is = new BufferedInputStream(is);
        PublicParameter pp = new PublicParameter();
        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry = zis.getNextEntry();
            assert Objects.requireNonNull(entry).getName().equals("pairingParam");
            ObjectInputStream ois = new ObjectInputStream(zis);
            pp.pairingParameters = (PairingParameters) ois.readObject();
            pp.e = PairingFactory.getPairing(pp.pairingParameters);

            entry = zis.getNextEntry();
            assert entry.getName().equals("t");

            pp.t = Integer.parseInt(new String(IOUtils.toByteArray(zis)));


            pp.p = pp.pairingParameters.getBigInteger("r");

            pp.g = deserializeElem(zis, pp.e, "g");
//            entry = zis.getNextEntry();
//            assert entry.getName().equals("g");
//            pp.g = pp.e.getG1().newElementFromBytes(IOUtils.toByteArray(zis));

            entry = zis.getNextEntry();
            assert entry.getName().equals("h");
            //noinspection unchecked
            pp.h = (BloomFilter<byte[]>) new ObjectInputStream(zis).readObject();

            return pp;
        } catch (IOException | ClassNotFoundException ioException) {
            ioException.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicParameter pp = (PublicParameter) o;
        return t == pp.t &&
                Objects.equals(e, pp.e) &&
                Objects.equals(h, pp.h) &&
                Objects.equals(p, pp.p) &&
                Objects.equals(g, pp.g) &&
                Objects.equals(pairingParameters, pp.pairingParameters);
    }
//        private HashSet<ElemWrap> hp;

    @Override
    public int hashCode() {
        return Objects.hash(e, h, p, g, t, pairingParameters);
    }

    public Pairing getE() {
        return e;
    }

    public void setE(Pairing e) {
        this.e = e;
    }

    public BigInteger getP() {
        return p;
    }

    public void setP(BigInteger p) {
        this.p = p;
    }

    public Element getG() {
        return g;
    }

    public void setG(Element g) {
        this.g = g;
    }

    public void serialize(OutputStream os) {
        if (!(os instanceof BufferedOutputStream))
            os = new BufferedOutputStream(os);

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(os)) {
            zipOutputStream.putNextEntry(new ZipEntry("pairingParam"));
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(zipOutputStream);
            objectOutputStream.writeObject(pairingParameters);
            objectOutputStream.flush();
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("t"));
            zipOutputStream.write(String.valueOf(t).getBytes());
            zipOutputStream.closeEntry();

            serializeElem(zipOutputStream, "g", g);
//            zipOutputStream.putNextEntry(new ZipEntry("g"));
//            zipOutputStream.write(g.toBytes());
//            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("h"));
            objectOutputStream = new ObjectOutputStream(zipOutputStream);
            objectOutputStream.writeObject(h);
            objectOutputStream.flush();
            zipOutputStream.closeEntry();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

//        public void seal() {
//            h = h.parallelStream().map(i -> {
//                i.rewind();
//                return i.asReadOnlyBuffer();
//            }).collect(Collectors.toSet());
//        }

    public void setPairingParameters(PairingParameters pairingParameters) {

        this.pairingParameters = pairingParameters;
    }

    public int getT() {
        return t;
    }

    public void setT(int t) {
        this.t = t;
    }

    public BloomFilter<byte[]> getH() {
        return h;
    }

    public void setH(BloomFilter<byte[]> h) {
        this.h = h;
    }

    //        public void setHp(HashSet<ElemWrap> hp) {
//            this.hp = hp;
//        }
//
//        public HashSet<ElemWrap> getHp() {
//            return hp;
//        }
}
