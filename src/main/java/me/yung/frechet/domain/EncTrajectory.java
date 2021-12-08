package me.yung.frechet.domain;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import me.yung.frechet.bb.IPSPEPointAdapter;
import org.apache.commons.compress.utils.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static me.yung.frechet.bb.ElemSerde.deserializeElem;
import static me.yung.frechet.bb.ElemSerde.serializeElem;

public class EncTrajectory {
    private final byte[] id;
    private final Element[][] traj;

    public EncTrajectory(byte[] id, Element[][] traj) {
        this.id = id;
        this.traj = traj;
    }

    public static EncTrajectory deserialize(byte[] bytes, Pairing pairing) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try (ZipInputStream zis = new ZipInputStream(bais)) {
            ZipEntry entry = zis.getNextEntry();
            assert Objects.requireNonNull(entry).getName().equals("id");
            byte[] id = IOUtils.toByteArray(zis);

            entry = zis.getNextEntry();
            assert Objects.requireNonNull(entry).getName().equals("traj");
            int len = zis.read();
            Element[][] traj = new Element[len][];
            for (int i = 0; i < len; i++) {
                entry = zis.getNextEntry();
                assert Objects.requireNonNull(entry).getName().equals(String.format("traj[%d]", i));
                int len1 = zis.read();
                traj[i] = new Element[len1];

                for (int j = 0; j < len1; j++) {
                    traj[i][j] = deserializeElem(zis, pairing, String.format("traj[%d][%d]", i, j));
                }
//                entry = zis.getNextEntry();
//                assert Objects.requireNonNull(entry).getName().equals(String.format("traj[%d][%d]", i, len1 - 1));
//                traj[i][len1 - 1] = pairing.getGT().newElementFromBytes(IOUtils.toByteArray(zis));
            }

            return new EncTrajectory(id, traj);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] serialize() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("id"));
            zos.write(id);
            zos.putNextEntry(new ZipEntry("traj"));
            zos.write(traj.length);
            for (int i = 0; i < traj.length; i++) {
                int len = traj[i].length;
                zos.putNextEntry(new ZipEntry(String.format("traj[%d]", i)));
                zos.write(len);
                for (int j = 0; j < traj[i].length; j++) {
                    serializeElem(zos, String.format("traj[%d][%d]", i, j), traj[i][j]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    public boolean verify(IPSPEPointAdapter pointAdapter, EncTrajectory trajB) {
        return verify(pointAdapter, trajB, 0, 0, new TreeMap<>());
    }

    private boolean proximityCheckWithCache(IPSPEPointAdapter pointAdapter, EncTrajectory trajB, int ptrA, int ptrB, TreeMap<Integer, Boolean> cache) {
        int key = this.traj.length * ptrA + ptrB;
        if (cache.containsKey(key))
            return cache.get(key);
        boolean ret = pointAdapter.check(this.traj[ptrA], trajB.traj[ptrB]);
        cache.put(key, ret);
        return ret;
    }

    private boolean verify(IPSPEPointAdapter pointAdapter, EncTrajectory trajB, int ptrA, int ptrB, TreeMap<Integer, Boolean> cache) {
        if (ptrA == this.traj.length && ptrB == trajB.traj.length) return true;
        if (ptrA >= this.traj.length || ptrB >= trajB.traj.length) return false;

        int key = this.traj.length * ptrA + ptrB + this.traj.length * trajB.traj.length;
        if (cache.containsKey(key))
            return cache.get(key);

        boolean ret = proximityCheckWithCache(pointAdapter, trajB, ptrA, ptrB, cache)
                && (verify(pointAdapter, trajB, ptrA + 1, ptrB + 1, cache)
                || verify(pointAdapter, trajB, ptrA + 1, ptrB, cache)
                || verify(pointAdapter, trajB, ptrA, ptrB + 1, cache));

        cache.put(key, ret);
        return ret;
    }
}
