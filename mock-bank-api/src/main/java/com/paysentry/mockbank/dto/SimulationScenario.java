package com.paysentry.mockbank.dto;

public enum SimulationScenario {
    NORMAL_ACTIVITY,
    LARGE_TRANSACTION,
    NEW_DEVICE_TRANSACTION,
    NEW_LOCATION_TRANSACTION,
    LIEN_CREATE,
    LIEN_INCREASE,
    DEVICE_CHANGE,
    LOCATION_CHANGE
}
