package com.access.careplanning;

public enum IntentEnum {

    SIGN_IN(0),
    USER_ID(1),
    USER_NAME(2),
    SCAN(3),
    ALARM(10),
    BATTERY_CHECK(11);

    private final int mCode;

    IntentEnum(int code) {
        mCode = code;
    }

    public int getCode() {
        return mCode;
    }
}
