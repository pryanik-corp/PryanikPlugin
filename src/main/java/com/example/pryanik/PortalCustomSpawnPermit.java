package com.example.pryanik;

public final class PortalCustomSpawnPermit {
    private static final ThreadLocal<Boolean> PERMITTED = ThreadLocal.withInitial(() -> false);
    private PortalCustomSpawnPermit() {}
    public static boolean isPermitted() { return PERMITTED.get(); }
    public static void run(Runnable action) {
        boolean prior = PERMITTED.get();
        PERMITTED.set(true);
        try { action.run(); }
        finally { PERMITTED.set(prior); }
    }
}
