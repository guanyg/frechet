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
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static me.yung.frechet.bb.Hasher.h;

public class EBranch extends ENode {

    private final Element[][] mbr;
    //    private ENode[] children;
    private final byte[][] children;

    public EBranch(Element[][] mbr, int numChildren) {
        this.mbr = mbr;
        this.children = new byte[numChildren][];
    }

    public EBranch(Element[][] mbr, byte[][] children) {
        this.mbr = mbr;
        this.children = children;
    }

    public static EBranch deserialize(byte[] bytes, Pairing pairing) {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try (ZipInputStream zis = new ZipInputStream(bais)) {
            Element[][] mbr = deserializeMBR(zis, pairing);
            byte[][] children = deserializeChildren(zis);

            return new EBranch(mbr, children);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[][] deserializeChildren(ZipInputStream zis) throws IOException {
        byte[][] children;
        ZipEntry entry = zis.getNextEntry();
        assert Objects.requireNonNull(entry).getName().equals("childPtr");
        int numChildren = zis.read();
        children = new byte[numChildren][];
        for (int i = 0; i < numChildren; i++) {
            entry = zis.getNextEntry();
            assert Objects.requireNonNull(entry).getName().equals(String.format("childPtr[%d]", i));
            children[i] = IOUtils.toByteArray(zis);
        }
        return children;
    }

    public void setChild(int i, byte[] child) {
        if (i >= 0 && i < children.length) {
            this.children[i] = child;
//            byte[][] data = child.serialize();
//            this.childPtr[i] = child;
        }
    }

    @Override
    protected Element[][] getMbr() {
        return mbr;
    }

    @Override
    public Set<ByteBuffer> query(Element[][] query, DB db, IPSPERectAdapter rectAdapter) {
        HashSet<ByteBuffer> ret = new HashSet<>();
        if (!rectAdapter.check(this.mbr, query))
            return ret;

        for (byte[] childptr : children) {
            if (childptr == null || childptr.length == 0) continue;
            ENode child = ENode.deserialize(db, childptr, rectAdapter.getPairing());
            if (child != null) {
                ret.addAll(child.query(query, db, rectAdapter));
            }
        }
        return ret;
    }

    @Override
    public byte[][] serialize() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            serilizeMbr(zos);

            zos.putNextEntry(new ZipEntry("childPtr"));
            zos.write(children.length);
            for (int i = 0; i < children.length; i++) {
                zos.putNextEntry(new ZipEntry(String.format("childPtr[%d]", i)));
                zos.write(children[i] == null ? new byte[0] : children[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] val = baos.toByteArray();

        byte[] key = h(val);
        key[0] |= 0x80;
        return new byte[][]{key, val};
    }
}
