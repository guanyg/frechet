package me.yung.frechet.rtree;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import me.yung.frechet.bb.IPSPERectAdapter;
import org.apache.commons.compress.utils.IOUtils;
import org.iq80.leveldb.DB;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static me.yung.frechet.bb.Hasher.h;

public class ELeaf extends ENode {
    private final Element[][] mbr;
    private final byte[] val;

    public ELeaf(Element[][] mbr, byte[] val) {
        this.val = val;
        this.mbr = mbr;
    }

    public static ELeaf deserialize(byte[] bytes, Pairing pairing) {

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try (ZipInputStream zis = new ZipInputStream(bais)) {
            Element[][] mbr = deserializeMBR(zis, pairing);

            ZipEntry entry = zis.getNextEntry();
            String name = null;
            try {
                name = Objects.requireNonNull(entry).getName();
                assert name.equals("val");
            } catch (AssertionError e) {
                System.err.println(name + " != val");

                throw e;
            }
            return new ELeaf(mbr, IOUtils.toByteArray(zis));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] getVal() {
        return val;
    }

    @Override
    protected Element[][] getMbr() {
        return mbr;
    }

    @Override
    public Set<ByteBuffer> query(Element[][] query, DB _db, IPSPERectAdapter rectAdapter) {
        if (intersect(query, rectAdapter)) {
            return Collections.singleton(ByteBuffer.wrap(val));
        }
        return Collections.emptySet();
    }

    @Override
    public byte[][] serialize() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            serilizeMbr(zos);
            zos.putNextEntry(new ZipEntry("val"));
            zos.write(val);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] val = baos.toByteArray();

        byte[] key = h(val);

        key[0] &= 0x7F;
        return new byte[][]{key, val};
    }
}
