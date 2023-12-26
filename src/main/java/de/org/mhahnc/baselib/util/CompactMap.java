package de.org.mhahnc.baselib.util;

public final class CompactMap<T,U> {
    final static int NOIDX = -1;

    Object[] data;

    public CompactMap() {
        this.data = new Object[0];
    }

    public CompactMap(CompactMap<T,U> cmap) {
        this.data = cmap.data.clone();
    }

    public void put(T t, U u) {
        int i = find(t);
        if (NOIDX == i) {
            Object[] d = this.data;
            this.data = new Object[d.length + 2];
            final int l = d.length;
            System.arraycopy(d, 0, this.data, 0, d.length);
            d = this.data;
            d[l    ] = t;
            d[l + 1] = u;
        }
        else {
            this.data[i] = u;
        }
    }

    @SuppressWarnings("unchecked")
    public U get(T t) {
        final Object[] d = this.data;
        final int i = null == t ? (0 == d.length ? NOIDX : 1) : find(t);
        return NOIDX == i ? null : (U)d[i];
    }

    private int find(T t) {
        final Object[] d = this.data;
        for (int i = 0, c = this.data.length; i < c; i += 2) {
            if (d[i].equals(t)) {
                return i + 1;
            }
        }
        return NOIDX;
    }
}
