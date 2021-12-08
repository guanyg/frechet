package me.yung.frechet;

import me.yung.frechet.domain.Trajectory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static me.yung.frechet.Main.DATA;

public abstract class AbstractBaselineScheme<P1, P2> implements Scheme<List<P2>, List<P1>> {
    private List<ArrayList<P1>> dataset;

    @Override
    public void loadDataset(Collection<Trajectory> trajectories) {
        this.dataset = trajectories.stream().map(i -> {
            ArrayList<P1> ret = new ArrayList<>();
            for (int j = 0; j < i.getPoints().length; j++) {
                ret.add(encP1(i.getPoints()[j]));
            }
            return ret;
        }).collect(Collectors.toList());
    }

    @Override
    public List<P2> tokenGen(Trajectory q, int epsilon) {
        long t4 = System.nanoTime();
        List<P2> ret = new ArrayList<>(q.getPoints().length);
        for (int i = 0; i < q.getPoints().length; i++) {
            ret.add(encP2(q.getPoints()[i], epsilon));
        }
        DATA("genToken-baseline", System.nanoTime() - t4);
        return ret;
    }

    @Override
    public List<List<P1>> query(List<P2> query) {
        long t5 = System.nanoTime();
        List<List<P1>> ret = dataset.stream().filter(i ->
                testDFD(i, query, 0, 0, new TreeMap<>())
        ).collect(Collectors.toList());
        DATA("query-baseline", System.nanoTime() - t5);
        return ret;
    }

    private boolean testDFD(ArrayList<P1> i, List<P2> q, int iP, int qP, TreeMap<Integer, Boolean> cache) {
        if (iP == i.size() && qP == q.size()) return true;
        if (iP >= i.size() || qP >= q.size()) return false;
        int key = i.size() * qP + iP + i.size() * q.size();
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        boolean ret = proximityTest(i, q, iP, qP, cache)
                && (testDFD(i, q, iP + 1, qP + 1, cache)
                || testDFD(i, q, iP, qP + 1, cache)
                || testDFD(i, q, iP + 1, qP, cache));

        cache.put(key, ret);
        return ret;
    }

    private boolean proximityTest(List<P1> i, List<P2> q, int iP, int qP, TreeMap<Integer, Boolean> cache) {
        int key = i.size() * qP + iP;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        boolean ret = proximityTest(i.get(iP), q.get(qP));
        cache.put(key, ret);
        return ret;
    }

    @Override
    public abstract void setup();

    protected abstract P1 encP1(int[] point);

    protected abstract P2 encP2(int[] point, int epsilon);

    protected abstract boolean proximityTest(P1 point1, P2 point2);
}
