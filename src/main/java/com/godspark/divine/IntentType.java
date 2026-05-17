package com.godspark.divine;

public enum IntentType {
    ORACLE_ONLY("Oracle Only"),
    ANSWER_QUESTION("Answer Question"),
    CREATE_TRIAL("Create Trial"),
    BLESS_COLONY("Bless Colony");

    private final String displayName;

    IntentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}