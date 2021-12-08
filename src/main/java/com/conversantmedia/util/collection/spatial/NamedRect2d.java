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

@SuppressWarnings({"rawtypes"})
public final class NamedRect2d implements HyperRect {
    final NamedPoint2d min, max;

    public NamedRect2d(final NamedPoint2d p) {
        min = new NamedPoint2d(p.x, p.y, new byte[0]);
        max = new NamedPoint2d(p.x, p.y, new byte[0]);
    }

    public NamedRect2d(final int x1, final int y1, final int x2, final int y2) {
        min = new NamedPoint2d(x1, y1, new byte[0]);
        max = new NamedPoint2d(x2, y2, new byte[0]);
    }

    public NamedRect2d(final NamedPoint2d p1, final NamedPoint2d p2) {
        final int minX, minY, maxX, maxY;

        if (p1.x < p2.x) {
            minX = p1.x;
            maxX = p2.x;
        } else {
            minX = p2.x;
            maxX = p2.x;
        }

        if (p1.y < p2.y) {
            minY = p1.y;
            maxY = p2.y;
        } else {
            minY = p2.y;
            maxY = p2.y;
        }

        min = new NamedPoint2d(minX, minY, new byte[0]);
        max = new NamedPoint2d(maxX, maxY, new byte[0]);
    }

    @Override
    public HyperRect getMbr(final HyperRect r) {
        final NamedRect2d r2 = (NamedRect2d) r;
        final int minX = Math.min(min.x, r2.min.x);
        final int minY = Math.min(min.y, r2.min.y);
        final int maxX = Math.max(max.x, r2.max.x);
        final int maxY = Math.max(max.y, r2.max.y);

        return new NamedRect2d(minX, minY, maxX, maxY);

    }

    @Override
    public int getNDim() {
        return 2;
    }

    @Override
    public HyperPoint getCentroid() {
        final int dx = min.x + ((max.x - min.x) >> 1);
        final int dy = min.y + ((max.y - min.y) >> 1);

        return new NamedPoint2d(dx, dy, new byte[0]);
    }

    @Override
    public HyperPoint getMin() {
        return min;
    }

    @Override
    public HyperPoint getMax() {
        return max;
    }

    @Override
    public double getRange(final int d) {
        if (d == 0) {
            return max.x - min.x;
        } else if (d == 1) {
            return max.y - min.y;
        } else {
            throw new IllegalArgumentException("Invalid dimension");
        }
    }

    @Override
    public boolean contains(final HyperRect r) {
        final NamedRect2d r2 = (NamedRect2d) r;

        return min.x <= r2.min.x &&
                max.x >= r2.max.x &&
                min.y <= r2.min.y &&
                max.y >= r2.max.y;
    }

    @Override
    public boolean intersects(final HyperRect r) {
        final NamedRect2d r2 = (NamedRect2d) r;

        return min.x <= r2.max.x &&
                r2.min.x <= max.x &&
                min.y <= r2.max.y &&
                r2.min.y <= max.y;
    }

    @Override
    public double cost() {
        final double dx = max.x - min.x;
        final double dy = max.y - min.y;
        return Math.abs(dx * dy);
    }

    @Override
    public double perimeter() {
        double p = 0.0;
        final int nD = this.getNDim();
        for (int d = 0; d < nD; d++) {
            p += 2.0 * this.getRange(d);
        }
        return p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final NamedRect2d rect2D = (NamedRect2d) o;

        return min.equals(rect2D.min) &&
                max.equals(rect2D.max);
    }

    @Override
    public int hashCode() {
        return min.hashCode() ^ 31 * max.hashCode();
    }

    public String toString() {

        return String.format("(%s,%s) (%s,%s)",
                Double.toString(min.x),
                Double.toString(min.y),
                Double.toString(max.x),
                Double.toString(max.y));
    }

    public final static class Builder implements RectBuilder<NamedRect2d> {

        @Override
        public HyperRect getBBox(final NamedRect2d rect2D) {
            return rect2D;
        }

        @Override
        public HyperRect getMbr(final HyperPoint p1, final HyperPoint p2) {
            return new NamedRect2d(p1.getCoord(0), p1.getCoord(1), p2.getCoord(0), p2.getCoord(1));
        }
    }
}