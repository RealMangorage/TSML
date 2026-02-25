package org.mangorage.tsml.api.mod;

public enum ModLoadingState {
    NOT_LOADED,
    SETUP, // Setting up classloaders
    MOD_DISCOVERY, // Finding TSMLLoaderAPI
    MOD_SCANNING_STATE,
    MOD_PRE_LOAD_STATE,
    MOD_LOADING_STATE, // Loading TSMLLoaderAPI
    LOADED,
    FAILED
}
