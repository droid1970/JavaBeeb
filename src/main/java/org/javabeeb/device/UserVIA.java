package org.javabeeb.device;

import org.javabeeb.util.StateKey;
import org.javabeeb.util.SystemStatus;

//
// Mostly based on  https://github.com/mattgodbolt/jsbeeb/via.js by Matt Godbolt
//
@StateKey(key = "userVIA")
public class UserVIA extends VIA {
    public UserVIA(
            final SystemStatus systemStatus,
            final String name,
            final int startAddress,
            final int size
    ) {
        super(systemStatus, name, startAddress, size);
    }
}
