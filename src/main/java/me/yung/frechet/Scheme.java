package me.yung.frechet;

import me.yung.frechet.domain.Trajectory;

import java.util.Collection;

public interface Scheme<Token, Traj> {
    void setup();

    void loadDataset(Collection<Trajectory> dataset);

    Token tokenGen(Trajectory q, int epsilon);

    Collection<Traj> query(Token token);
}
