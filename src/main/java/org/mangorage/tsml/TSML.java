package org.mangorage.tsml;

import org.mangorage.tsml.api.Environment;

public final class TSML {
    public static Environment getEnvironment() {
        return org.mangorage.tsml.internal.TSML.getEnvironment();
    }
}
