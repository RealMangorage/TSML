package org.mangorage.tsml.api.mod;

public enum ModLoadingState {
    NOT_LOADED,
    INITIAL_SETUP, // Setting up classloaders
    CONFIGURATION_SETUP,
    MOD_DISCOVERY, // Finding TSMLLoaderAPI
    MOD_SCANNING,
    MOD_PRE_LOAD,
    MOD_LOADING, // Loading TSMLLoaderAPI
    LOADING_STATE,
    FINISHED,
    FAILED
}
