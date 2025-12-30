/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig
 */
package cgeo.geocaching.wherigo.openwig;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Marker interface for objects that can be serialized to game saves.
 * <p>
 * Extends java.io.Serializable and adds explicit serialize/deserialize methods
 * for custom binary serialization used by OpenWIG's save game system.
 */
public interface Serializable extends java.io.Serializable {
    void serialize (DataOutputStream out) throws IOException;
    void deserialize (DataInputStream in) throws IOException;
}
