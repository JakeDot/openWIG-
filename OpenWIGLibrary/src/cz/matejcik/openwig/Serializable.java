package cz.matejcik.openwig;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Serializable {

    default void serialize (DataOutputStream out) throws IOException {
        Engine.instance.savegame.storeValue(this, out);
    }

    default void deserialize (DataInputStream in) throws IOException {
        Engine.instance.savegame.restoreValue(in, this);
    }
}
