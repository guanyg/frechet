package me.yung.frechet.domain;

import com.conversantmedia.util.collection.spatial.NamedPoint2d;

import java.io.Serializable;

public class Trajectory implements Serializable {
    public static final int X = 0;
    public static final int Y = 1;
    private final int[][] points;
    private final byte[] id;

    public Trajectory(int[][] points, byte[] id) {
        this.points = points;
        this.id = id;
    }

    public int[][] getPoints() {
        return points;
    }

    public byte[] getId() {
        return id;
    }

    public NamedPoint2d[] getMBR() {
        int xmin = Integer.MAX_VALUE;
        int ymin = Integer.MAX_VALUE;
        int xmax = 0;
        int ymax = 0;
        for (int[] point : points) {
            if (xmin > point[X]) {
                xmin = point[X];
            }
            if (ymin > point[Y]) {
                ymin = point[Y];
            }
            if (xmax < point[X]) {
                xmax = point[X];
            }
            if (ymax < point[Y]) {
                ymax = point[Y];
            }
        }
        return new NamedPoint2d[]{
                new NamedPoint2d(xmin, ymin, id),
                new NamedPoint2d(xmax, ymax, id)
        };
    }
}
