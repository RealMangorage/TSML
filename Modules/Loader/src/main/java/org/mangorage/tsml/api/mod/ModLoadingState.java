package org.mangorage.tsml.api.mod;

public enum ModLoadingState {
    NOT_LOADED,
    INITIAL_SETUP,
    MOD_DISCOVERY,
    MOD_SCANNING,
    MOD_PRE_LOAD,
    MOD_LOADING,
    FINISHED,
    FAILED
}
