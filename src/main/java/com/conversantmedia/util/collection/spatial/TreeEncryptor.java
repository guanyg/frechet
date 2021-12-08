package com.conversantmedia.util.collection.spatial;

import it.unisa.dia.gas.jpbc.Element;
import me.yung.frechet.bb.IPSPE;
import me.yung.frechet.bb.IPSPERectAdapter;
import me.yung.frechet.rtree.EBranch;
import me.yung.frechet.rtree.ELeaf;
import me.yung.frechet.rtree.ENode;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import static me.yung.frechet.Main.DATA;
import static org.iq80.leveldb.impl.Iq80DBFactory.bytes;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

@SuppressWarnings({"rawtypes"})
public class TreeEncryptor {
    static NamedPoint2d.Builder builder = new NamedPoint2d.Builder();
    private final IPSPERectAdapter rectAdapter;
    private final DB db;

    public TreeEncryptor(IPSPE ipspe, BigInteger[] gamma, String dbName) throws IOException {
        this.rectAdapter = new IPSPERectAdapter(ipspe, gamma);

        Options options = new Options();
        options.createIfMissing(true);
        this.db = factory.open(new File(dbName), options);
    }

    public DB convert(RTree<NamedPoint2d> rTree) {
        db.put(bytes("root"), encAndSave(rTree.getRoot()));
        return db;
    }

    private Element[][] encrypt(HyperRect rectHR) {
        NamedRect2d rect = (NamedRect2d) rectHR;
        NamedPoint2d min = rect.min;
        NamedPoint2d max = rect.max;
        return rectAdapter.encrypt(
                new int[]{min.x, min.y},
                new int[]{max.x, max.y});
    }

    private byte[] encAndSave(Node node) {
        if (node instanceof Branch) {
            Branch branch = (Branch) node;
            Node[] children = branch.getChildren();
            byte[][] childPtrs = new byte[children.length][];
            for (int i = 0; i < children.length; i++) {
                childPtrs[i] = encAndSave(children[i]);
            }
            return saveNode(new EBranch(encrypt(branch.getBound()), childPtrs));
        } else if (node instanceof Leaf) {
            Leaf leaf = (Leaf) node;

            if (leaf.size() == 1) {
                NamedPoint2d point = (NamedPoint2d) leaf.entry[0];
                return saveNode(new ELeaf(encrypt(builder.getBBox(point)), point.id));
            } else {
                byte[][] childPtrs = new byte[leaf.size][];
                for (int i = 0; i < leaf.size(); i++) {
                    NamedPoint2d point = (NamedPoint2d) leaf.entry[i];
                    ELeaf newLeaf = new ELeaf(encrypt(builder.getBBox(point)), point.id);
                    childPtrs[i] = saveNode(newLeaf);
                }
                return saveNode(new EBranch(encrypt(leaf.mbr), childPtrs));
            }
        }
        return new byte[0];
    }

    private byte[] saveNode(ENode node) {
        long t0 = System.nanoTime();
        byte[][] sNode = node.serialize();
//        assert db.get(sNode[0]) == null;
        db.put(sNode[0], sNode[1]);
        DATA("saveNode", System.nanoTime() - t0);
        return sNode[0];
    }
}
