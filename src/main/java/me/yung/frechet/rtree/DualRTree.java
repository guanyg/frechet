package me.yung.frechet.rtree;

import com.conversantmedia.util.collection.spatial.*;
import it.unisa.dia.gas.jpbc.Element;
import me.yung.frechet.bb.IPSPE;
import me.yung.frechet.bb.IPSPEPointAdapter;
import me.yung.frechet.bb.IPSPERectAdapter;
import me.yung.frechet.domain.EncTrajectory;
import me.yung.frechet.domain.Trajectory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static me.yung.frechet.Main.DATA;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

public class DualRTree {
    private final SpatialSearch<NamedPoint2d> lTree, uTree;
    private final HashMap<ByteBuffer, Trajectory> trajs;

    public DualRTree() {
        lTree = SpatialSearches.rTree(new NamedPoint2d.Builder());
        uTree = SpatialSearches.rTree(new NamedPoint2d.Builder());

        trajs = new HashMap<>();
    }

    public void put(Trajectory trajectory) {
        NamedPoint2d[] mbr = trajectory.getMBR();
        lTree.add(mbr[0]);
        uTree.add(mbr[1]);
        trajs.put(ByteBuffer.wrap(trajectory.getId()), trajectory);
    }

    public EncDualRTree enc(IPSPE ipspe, String dbPath) throws IOException {
        DB eLTree = new TreeEncryptor(ipspe, ipspe.getEk().getGamma1(), dbPath + "/encLTree").convert((RTree<NamedPoint2d>) lTree);
        DB eUTree = new TreeEncryptor(ipspe, ipspe.getEk().getGamma2(), dbPath + "/encUTree").convert((RTree<NamedPoint2d>) uTree);
        DB eTrajs = factory.open(new File(dbPath + "/encTrajs"), new Options().createIfMissing(true));

        IPSPEPointAdapter pointAdapter = new IPSPEPointAdapter(ipspe);
        for (Map.Entry<ByteBuffer, Trajectory> entry : trajs.entrySet()) {
            EncTrajectory encTrajectory = encTraj(pointAdapter, entry.getValue());

            long t0 = System.nanoTime();
            eTrajs.put(entry.getKey().array(), encTrajectory.serialize());
            DATA("saveTraj", System.nanoTime() - t0);
        }

        return new EncDualRTree(eLTree, eUTree, eTrajs, new IPSPERectAdapter(ipspe));
    }

    private EncTrajectory encTraj(IPSPEPointAdapter pointAdapter, Trajectory traj) {
        int[][] points = traj.getPoints();
        Element[][] ret = new Element[points.length][];
        for (int i = 0; i < points.length; i++) {
            ret[i] = pointAdapter.encrypt(points[i]);
        }
        return new EncTrajectory(traj.getId(), ret);
    }
}
