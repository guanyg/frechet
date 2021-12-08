package me.yung.frechet.rtree;

import it.unisa.dia.gas.jpbc.Element;
import me.yung.frechet.bb.IPSPERectAdapter;
import me.yung.frechet.domain.EncTrajectory;
import org.iq80.leveldb.DB;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static me.yung.frechet.Main.DATA;
import static org.iq80.leveldb.impl.Iq80DBFactory.bytes;

public class EncDualRTree {
    private final DB eLTree;
    private final ENode eLTreeRoot;
    private final DB eUTree;
    private final ENode eUTreeRoot;
    private final DB eTrajs;
    private final IPSPERectAdapter rectAdapter;

    public EncDualRTree(DB eLTree, DB eUTree, DB eTrajs, IPSPERectAdapter rectAdapter) {
        this.eLTree = eLTree;
        this.eUTree = eUTree;
        this.eTrajs = eTrajs;

        this.eLTreeRoot = ENode.deserialize(eLTree, eLTree.get(bytes("root")), rectAdapter.getPairing());
        this.eUTreeRoot = ENode.deserialize(eUTree, eUTree.get(bytes("root")), rectAdapter.getPairing());

        this.rectAdapter = rectAdapter;
    }

    public List<EncTrajectory> query(Element[][] tokenL, Element[][] tokenU) {
        long t0 = System.nanoTime();
        Set<ByteBuffer> l = eLTreeRoot.query(tokenL, eLTree, rectAdapter);
        Set<ByteBuffer> u = eUTreeRoot.query(tokenU, eUTree, rectAdapter);
        l.retainAll(u);
        DATA("filter", System.nanoTime() - t0);

        long t1 = System.nanoTime();
        List<EncTrajectory> list = new ArrayList<>();
        for (ByteBuffer i : l) {
            EncTrajectory deserialize = EncTrajectory.deserialize(eTrajs.get(i.array()), rectAdapter.getPairing());
            list.add(deserialize);
        }
        DATA("readTraj", System.nanoTime() - t1);
        return list;
    }
}
