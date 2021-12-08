package me.yung.frechet;

import me.yung.frechet.domain.Trajectory;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"rawtypes", "unchecked"})
@CommandLine.Command(name = "testdfd", mixinStandardHelpOptions = true)
public class Main implements Callable<Integer> {
    @CommandLine.Option(names = {"--keyfile"})
    private String keyfile = "keys";

    @CommandLine.Option(names = {"--dataset"})
    private String dataset = "/Users/yung/OneDrive - University of New Brunswick/datasets/files/";

    @CommandLine.Option(names = {"--dbpath"})
    private String dbpath = "data";

    @CommandLine.Option(names = {"-n"}, split = ",")
    private int[] nArr = new int[]{50};

    @CommandLine.Option(names = {"-e"}, split = ",")
    private int[] epsilonArr = new int[]{5};

    @CommandLine.Option(names = {"-l"}, split = ",")
    private int[] lArr = new int[]{5};

    @CommandLine.Option(names = "--repeat-enc")
    private int repeatEnc = 5;

    @CommandLine.Option(names = "--repeat-query")
    private int repeatQuery = 50;

    @CommandLine.Option(names = "-t")
    private int t = 30000;

    @CommandLine.Option(names = "-s")
    private SchemeOp schemeOp = SchemeOp.PROPOSAL;

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    public static void DATA(String key, long value) {
        System.out.printf("DATA>> %s %d%n", key, value);
    }

    @Override
    public Integer call() {
        Scheme scheme = switch (schemeOp) {
            case PROPOSAL -> new DFDRangeQuery.DFDRangeQueryScheme(keyfile, t, dbpath);
            case BASELINE -> new BaselineScheme(keyfile, t);
        };
        scheme.setup();

        for (int n : nArr) {
            DATA("n", n);
            for (int l : lArr) {
                DATA("l", l);

                TrajectorySupplier trajSupplier = new TrajectorySupplier(l);

                List<Trajectory> trajectories = Stream.generate(trajSupplier)
                        .limit(n).collect(Collectors.toList());

                for (int i = 0; i < repeatEnc; i++) {
                    scheme.loadDataset(trajectories);
                }

                for (int epsilon : epsilonArr) {
                    DATA("epsilon", epsilon);
                    Collections.shuffle(trajectories);

                    int i = 0;
                    assert trajectories.size() > repeatQuery;
                    for (Trajectory trajectory : trajectories) {
                        if (++i > repeatQuery) break;
                        var token = scheme.tokenGen(trajectory, epsilon);
                        scheme.query(token);
                    }
                }
            }
        }

        return 0;
    }

    private class TrajectorySupplier implements Supplier<Trajectory> {
        private final int l;
        private Iterator<String> it;
        private int innerCtr = 0;
        private Scanner scanner;
        private String curFile;

        public TrajectorySupplier(int l) {
            this.l = l;

            it = getDatafileIt();
        }

        private Iterator<String> getDatafileIt() {
            return Arrays.stream(Objects.requireNonNull(new File(dataset).list()))
                    .filter(i -> i.startsWith("file-"))
                    .collect(Collectors.collectingAndThen(
                            Collectors.toCollection(ArrayList::new),
                            list -> {
                                Collections.shuffle(list);
                                return list;
                            }))
                    .iterator();
        }

        @Override
        public Trajectory get() {
            if (scanner == null) {
                if (!it.hasNext()) it = getDatafileIt();
                if (!it.hasNext()) return null;
                try {
                    innerCtr = 0;
                    curFile = it.next();
                    scanner = new Scanner(new FileInputStream(dataset + "/" + curFile));
                    scanner.nextLine();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            int[][] ret = new int[l][];
            for (int i = 0; i < l; i++) {
                if (!scanner.hasNext()) {
                    scanner.close();
                    scanner = null;
                    return this.get();
                }
                String[] line = scanner.nextLine().split(" ");

                int yoffset = 4509960;
                int xoffset = -13651497;
                ret[i] = new int[]{
                        (int) (Float.parseFloat(line[0]) - xoffset) / 200,
                        (int) (Float.parseFloat(line[1]) - yoffset) / 200};
            }
            innerCtr++;
            String id = String.format("%s-%4d", curFile, innerCtr);
            return new Trajectory(ret, id.getBytes(StandardCharsets.UTF_8));
        }

    }
}
