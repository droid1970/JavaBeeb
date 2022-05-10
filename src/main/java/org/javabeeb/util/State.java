package org.javabeeb.util;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public final class State {

    private final Map<String, TypedProperties> map = new HashMap<>();

    public void put(final String key, final TypedProperties value) {
        map.put(key, value);
    }

    public TypedProperties get(String key) {
        return map.get(key);
    }

    public void write(final File file) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            write(out);
        }
    }

    public static State read(final File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            return read(in);
        }
    }

    public void write(final DataOutput out) throws IOException {
        out.writeInt(map.size());
        for (Map.Entry<String, TypedProperties> e : map.entrySet()) {
            out.writeUTF(e.getKey());
            e.getValue().write(out);
        }
    }

    public static State read(final DataInput in) throws IOException {
        final State ret = new State();
        final int size = in.readInt();
        for (int i = 0; i < size; i++) {
            ret.put(in.readUTF(), TypedProperties.read(in));
        }
        return ret;
    }
}
