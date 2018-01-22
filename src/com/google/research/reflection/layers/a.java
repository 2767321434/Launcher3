package com.google.research.reflection.layers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class a {
    List<v> Nh;
    com.google.research.reflection.common.a Ni;
    com.google.research.reflection.common.a Nj;

    public a(final int n) {
        final boolean b = true;
        this.Nh = new ArrayList<>();
        this.Nj = new com.google.research.reflection.common.a(n, b);
        this.Ni = new com.google.research.reflection.common.a(n, b);
    }

    private void TG() {
        for (int i = 0; i < this.Nh.size(); ++i) {
            this.Nh.get(i).UC();
        }
    }

    public void TE() {
        this.Nj.clear();
        this.Ni.clear();
        for (v aNh : this.Nh) {
            aNh.UO();
        }
    }

    public List<v> TF() {
        return this.Nh;
    }

    public void TH(final b b, final b b2, final int n, final boolean b3) throws InvalidValueException {
        final boolean b4 = true;
        if (b3 && !(this.Nh.get(this.Nh.size() - 1) instanceof m)) {
            throw new RuntimeException("Lacks outputlayer");
        }
        if (b != null) {
            this.Nj.add(b);
            this.Ni.add(b2);
            final int sy = this.Nj.SY();
            this.TG();
            final int sv = this.Nj.SV();
            final int size = this.Nh.size();
            for (int i = size - 1; i >= 0; --i) {
                final v v = this.Nh.get(i);
                if (!v.Pd) {
                    if (v.Pb.SY() != sy) {
                        throw new RuntimeException("backward: dense input vector has a different frame index from the target frame index: " + v.Pb.SY() + "!=" + sy);
                    }
                } else if (v.Pc.SY() != sy) {
                    throw new RuntimeException("backward: sparse input vector has a different frame index from the target frame index");
                }
            }
            for (int n2 = sv - 1; n2 >= 0 && sv - 1 - n2 < n; --n2) {
                final b b5 = (b) this.Nj.ST(n2);
                final b b6 = (b) this.Ni.ST(n2);
                int j = size - 1;
                b uy = b5;
                while (j >= 0) {
                    final v v2 = this.Nh.get(j);
                    v2.UD(this, n2, uy, v2.UU(), b6);
                    uy = v2.UY();
                    --j;
                }
            }
            return;
        }
        final b b7 = new b(1, this.TN().UX());
        final b b8 = new b(1, this.TN().UX());
        this.Nj.add(b7);
        this.Ni.add(b8);
    }

    public int TI() {
        return this.Nh.get(0).UT();
    }

    public b TJ(final boolean b, ArrayList[] array, b b2, final boolean b3) throws InvalidValueException {
        if (b3 && !(this.Nh.get(this.Nh.size() - 1) instanceof m)) {
            throw new RuntimeException("Lacks outputlayer");
        }
        int i = 0;
        b b4 = null;
        while (i < this.Nh.size()) {
            final b ub = this.Nh.get(i).UB(b, this, array, b2);
            ++i;
            b2 = ub;
            array = null;
            b4 = ub;
        }
        return b4;
    }

    public void TK(final v v) {
        if (v.US() != this.Nj.SX()) {
            throw new RuntimeException("Inconsistent framebuffer size with the added layer: targetsize=" + this.Nj.SX() + " layerbuffersize=" + v.US());
        }
        if (v instanceof o && ((o) v).UA() == this.Nh.size()) {
            throw new RuntimeException();
        }
        this.Nh.add(v);
    }

    public void TL(final DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeInt(this.Nh.size());
        for (final com.google.research.reflection.layers.v v : this.Nh) {
            dataOutputStream.writeUTF(v.Uu());
            v.Uv(dataOutputStream);
        }
        dataOutputStream.writeUTF("NeuralNet");
    }

    public void TM(final DataInputStream dataInputStream) throws IOException {
        this.Nh.clear();
        for (int int1 = dataInputStream.readInt(), i = 0; i < int1; ++i) {
            final String utf = dataInputStream.readUTF();
            v v;
            if (!utf.equals("LinearLayer")) {
                if (!utf.equals("OutputLayer")) {
                    if (!utf.equals("LSTMLayer")) {
                        final String value = String.valueOf(utf);
                        final int length = value.length();
                        final String s = "Unsupported layer type: ";
                        String concat;
                        if (length == 0) {
                            concat = s;
                        } else {
                            concat = s.concat(value);
                        }
                        throw new IOException(concat);
                    }
                    v = new s();
                } else {
                    v = new m();
                }
            } else {
                v = new o();
            }
            v.Uw(dataInputStream);
            this.Nh.add(v);
        }
        final String utf2 = dataInputStream.readUTF();
        if (utf2.equals("NeuralNet")) {
            return;
        }
        throw new IOException("Inconsistent ending: [" + utf2 + "] expected: [NeuralNet]");
    }

    public v TN() {
        return this.Nh.get(this.Nh.size() - 1);
    }

    public a clone() {
        final a a = new a(this.Nj.SX());
        for (v aNh : this.Nh) {
            a.Nh.add(aNh.clone());
        }
        return a;
    }

    public void update() throws InvalidValueException {
        for (int i = this.Nh.size() - 1; i >= 0; --i) {
            this.Nh.get(i).update();
        }
    }
}
