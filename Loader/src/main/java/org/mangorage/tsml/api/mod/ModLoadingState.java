package org.mangorage.tsml.api.mod;

public enum ModLoadingState {
    NOT_LOADED,
    SETUP, // Setting up classloaders
    MOD_DISCOVERY, // Finding TSMLLoaderAPI
    MOD_SCANNING,
    MOD_PRE_LOAD,
    MOD_LOADING, // Loading TSMLLoaderAPI
    LOADING_STATE,
    FAILED
}
