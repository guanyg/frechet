package me.yung.frechet;

import it.unisa.dia.gas.jpbc.Element;
import me.yung.frechet.bb.IPSPEPointAdapter;

import static me.yung.frechet.DFDRangeQuery.keygen;

public class BaselineScheme extends AbstractBaselineScheme<Element[], Element[]> {
    private final String keyfile;
    private final int t;
    private IPSPEPointAdapter ipspe;

    public BaselineScheme(String keyfile, int t) {
        this.keyfile = keyfile;
        this.t = t;
    }

    @Override
    public void setup() {
        if (ipspe == null)
            this.ipspe = new IPSPEPointAdapter(keygen(keyfile, t));
    }

    @Override
    protected Element[] encP1(int[] point) {
        return ipspe.encrypt(point);
    }

    @Override
    protected Element[] encP2(int[] point, int epsilon) {
        return ipspe.tokenGen(point, epsilon);
    }

    @Override
    protected boolean proximityTest(Element[] point1, Element[] point2) {
        return ipspe.check(point1, point2);
    }
}
