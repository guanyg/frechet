package com.conversantmedia.util.collection.spatial;

/*
 * #%L
 * Conversant RTree
 * ~~
 * Conversantmedia.com © 2016, Conversant, Inc. Conversant® is a trademark of Conversant, Inc.
 * ~~
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 * Created by jcovert on 6/15/15.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class NamedPoint2d implements HyperPoint {
    public static final int X = 0;
    public static final int Y = 1;

    final int x, y;
    public byte[] id;

    public NamedPoint2d(final int x, final int y, byte[] id) {
        this.x = x;
        this.y = y;
        this.id = id;
    }

    @Override
    public int getNDim() {
        return 2;
    }

    @Override
    public Integer getCoord(final int d) {
        if (d == X) {
            return x;
        } else if (d == Y) {
            return y;
        } else {
            throw new IllegalArgumentException("Invalid dimension");
        }
    }

    @Override
    public double distance(final HyperPoint p) {
        final NamedPoint2d p2 = (NamedPoint2d) p;

        final double dx = p2.x - x;
        final double dy = p2.y - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public double distance(final HyperPoint p, final int d) {
        final NamedPoint2d p2 = (NamedPoint2d) p;
        if (d == X) {
            return Math.abs(p2.x - x);
        } else if (d == Y) {
            return Math.abs(p2.y - y);
        } else {
            throw new IllegalArgumentException("Invalid dimension");
        }
    }

    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final NamedPoint2d p = (NamedPoint2d) o;
        return RTree.isEqual(x, p.x) &&
                RTree.isEqual(y, p.y);
    }


    public int hashCode() {
        return Double.hashCode(x) ^ 31 * Double.hashCode(y);
    }

    public final static class Builder implements RectBuilder<NamedPoint2d> {

        @Override
        public HyperRect getBBox(final NamedPoint2d point) {
            return new NamedRect2d(point);
        }

        @Override
        public HyperRect getMbr(final HyperPoint p1, final HyperPoint p2) {
            final NamedPoint2d point1 = (NamedPoint2d) p1;
            final NamedPoint2d point2 = (NamedPoint2d) p2;
            return new NamedRect2d(point1, point2);
        }
    }
}