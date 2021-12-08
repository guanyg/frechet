package me.yung.frechet.rtree;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import me.yung.frechet.bb.IPSPERectAdapter;
import org.iq80.leveldb.DB;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static me.yung.frechet.Main.DATA;
import static me.yung.frechet.bb.ElemSerde.deserializeElem;
import static me.yung.frechet.bb.ElemSerde.serializeElem;

public abstract class ENode {
    public static ENode deserialize(DB db, byte[] bytes, Pairing pairing) {
        ENode node;
        long t0 = System.nanoTime();
        if (bytes[0] < 0) {
            node = EBranch.deserialize(db.get(bytes), pairing);
        } else {
            node = ELeaf.deserialize(db.get(bytes), pairing);
        }
        DATA("readNode", System.nanoTime() - t0);

        return node;
    }

    static Element[][] deserializeMBR(ZipInputStream zis, Pairing pairing) throws IOException {
        Element[][] ret = new Element[0][];

        ZipEntry entry = zis.getNextEntry();
        assert entry != null;
        if (entry.getName().equals("mbr")) {
            int lenArr = zis.read();
            ret = new Element[lenArr][];

            for (int i = 0; i < lenArr; i++) {
                entry = zis.getNextEntry();
                assert entry.getName().equals(String.format("mbr[%d]", i));
                int lenArr2 = zis.read();
                ret[i] = new Element[lenArr2];

                for (int j = 0; j < lenArr2; j++) {
                    ret[i][j] = deserializeElem(zis, pairing, String.format("mbr[%d][%d]", i, j));
                }
            }
        }
        return ret;
    }

    protected abstract Element[][] getMbr();

    protected boolean intersect(Element[][] query, IPSPERectAdapter rectAdapter) {
        return rectAdapter.check(getMbr(), query);
    }

    public abstract Set<ByteBuffer> query(Element[][] query, DB db, IPSPERectAdapter rectAdapter);

    public abstract byte[][] serialize();

    void serilizeMbr(ZipOutputStream zos) throws IOException {
        zos.putNextEntry(new ZipEntry("mbr"));
        zos.write(getMbr().length);
        for (int i = 0; i < getMbr().length; i++) {
            zos.putNextEntry(new ZipEntry(String.format("mbr[%d]", i)));
            zos.write(getMbr()[i].length);
            for (int j = 0; j < getMbr()[i].length; j++) {
                String entryName = String.format("mbr[%d][%d]", i, j);
                Element element = getMbr()[i][j];
                serializeElem(zos, entryName, element);
            }
        }
    }
}
