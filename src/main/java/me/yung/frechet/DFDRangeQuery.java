package me.yung.frechet;

import com.conversantmedia.util.collection.spatial.NamedPoint2d;
import it.unisa.dia.gas.jpbc.Element;
import me.yung.frechet.bb.*;
import me.yung.frechet.domain.EncToken;
import me.yung.frechet.domain.EncTrajectory;
import me.yung.frechet.domain.Trajectory;
import me.yung.frechet.rtree.DualRTree;
import me.yung.frechet.rtree.EncDualRTree;
import org.jlinalg.field_p.FieldPBigFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static me.yung.frechet.Main.DATA;

public class DFDRangeQuery {
    /**
     * Load or generate the secret keys for IPSPE
     *
     * @param keyfile the path of the secret keys
     * @param t       the maximum value for inner-products
     * @return secret keys
     */
    static IPSPE keygen(String keyfile, int t) {
        int kappa = 512;
        int k = 2;

        keyfile = String.format("%s_%d_%d_%d", keyfile, kappa, k, t);
        IPSPE ipspe = IPSPE.load(keyfile);
        if (ipspe == null) {
            ipspe = IPSPE.keyGeneration(kappa, k, t);
            ipspe.serialize(keyfile);
        }

        return ipspe;
    }

    /**
     * Build a dual R-tree index from a group of trajectories
     *
     * @param trajectories original trajectories
     * @return a plaintext dual R-tree index
     */
    static DualRTree buildIndex(Collection<Trajectory> trajectories) {
        DualRTree ret = new DualRTree();
        for (Trajectory t : trajectories) {
            ret.put(t);
        }
        return ret;
    }

    /**
     * Encrypt the dual R-tree index
     *
     * @param pp        the public parameters
     * @param ek        the encryption key
     * @param dualRTree the plaintext index
     * @param dbPath    the path of the encrypted index
     * @return an encrypted index
     * @throws IOException may fail to write the index to the disk
     */
    static EncDualRTree encIndex(PublicParameter pp, EncryptionKey ek, DualRTree dualRTree, String dbPath) throws IOException {
        IPSPE ipspe = new IPSPE(pp, (FieldPBigFactory) ek.getM1().getFactory());
        ipspe.setEk(ek);
        return dualRTree.enc(ipspe, dbPath);
    }

    /**
     * Generate a query token
     *
     * @param pp         the public parameters
     * @param tk         the token generation key
     * @param trajectory the query trajectory
     * @param epsilon    the threshold
     * @return an encrypted query token
     */
    static EncToken tokenGen(PublicParameter pp, TokenKey tk, Trajectory trajectory, int epsilon) {
        IPSPE ipspe = new IPSPE(pp, (FieldPBigFactory) tk.getM1().getFactory());
        ipspe.setTk(tk);
        IPSPERectAdapter rectAdapterL = new IPSPERectAdapter(ipspe, ipspe.getTk().getGamma1());
        IPSPERectAdapter rectAdapterU = new IPSPERectAdapter(ipspe, ipspe.getTk().getGamma2());
        NamedPoint2d[] mbr = trajectory.getMBR();

        Element[][] tokenL = rectAdapterL.tokenGen(
                new int[]{mbr[0].getCoord(NamedPoint2d.X) - epsilon,
                        mbr[0].getCoord(NamedPoint2d.Y) - epsilon},
                new int[]{mbr[0].getCoord(NamedPoint2d.X) + epsilon,
                        mbr[0].getCoord(NamedPoint2d.Y) + epsilon}
        );
        Element[][] tokenU = rectAdapterU.tokenGen(
                new int[]{mbr[1].getCoord(NamedPoint2d.X) - epsilon,
                        mbr[1].getCoord(NamedPoint2d.Y) - epsilon},
                new int[]{mbr[1].getCoord(NamedPoint2d.X) + epsilon,
                        mbr[1].getCoord(NamedPoint2d.Y) + epsilon}
        );

        IPSPEPointAdapter pointAdapter = new IPSPEPointAdapter(ipspe);
        int[][] points = trajectory.getPoints();

        Element[][] encTraj = IntStream.range(0, points.length).parallel()
                .mapToObj(i -> new AbstractMap.SimpleEntry<>(i, pointAdapter.tokenGen(points[i], epsilon)))
                .sorted(java.util.Map.Entry.comparingByKey()).map(Map.Entry::getValue).toArray(Element[][]::new);

        return new EncToken(
                tokenL,
                tokenU,
                new EncTrajectory(trajectory.getId(), encTraj)
        );
    }

    /**
     * Query over the encrypted index
     *
     * @param pp           the public parameters
     * @param encDualRTree the encrypted index
     * @param encToken     the encrypted query token
     * @return the query result
     */
    static Set<EncTrajectory> query(PublicParameter pp, EncDualRTree encDualRTree, EncToken encToken) {
        IPSPE ipspe = new IPSPE(pp);
        IPSPEPointAdapter pointAdapter = new IPSPEPointAdapter(ipspe);
        List<EncTrajectory> query = encDualRTree.query(encToken.getTokenL(), encToken.getTokenU());
        DATA("numCandidates", query.size());
        return query.stream()
                .filter(traj -> traj.verify(pointAdapter, encToken.getEncTraj()))
                .collect(Collectors.toSet());
    }

    public static class DFDRangeQueryScheme implements Scheme<EncToken, EncTrajectory> {
        private final String keyfile;
        private final int t;
        private final String dbPath;
        private IPSPE ipspe;
        private EncDualRTree encIndex;

        public DFDRangeQueryScheme(String keyfile, int t, String dbPath) {
            this.keyfile = keyfile;
            this.t = t;
            this.dbPath = dbPath;
        }

        @Override
        public void setup() {
            this.ipspe = keygen(keyfile, t);
        }

        @Override
        public void loadDataset(Collection<Trajectory> dataset) {
            long t0 = System.nanoTime();
            DualRTree index = buildIndex(dataset);
            DATA("buildIndex", System.nanoTime() - t0);
            try {
                long t1 = System.nanoTime();
                this.encIndex = encIndex(ipspe.getPp(), ipspe.getEk(), index, dbPath + t0);
                DATA("encIndex", System.nanoTime() - t1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public EncToken tokenGen(Trajectory trajectory, int epsilon) {
            long t2 = System.nanoTime();
            EncToken encToken = DFDRangeQuery.tokenGen(ipspe.getPp(), ipspe.getTk(), trajectory, epsilon);
            DATA("genToken", System.nanoTime() - t2);
            return encToken;
        }

        @Override
        public Set<EncTrajectory> query(EncToken encToken) {
            long t3 = System.nanoTime();
            Set<EncTrajectory> query = DFDRangeQuery.query(ipspe.getPp(), encIndex, encToken);
            DATA("query", System.nanoTime() - t3);
            return query;
        }
    }
}
