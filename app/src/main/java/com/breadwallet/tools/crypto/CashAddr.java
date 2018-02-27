package com.breadwallet.tools.crypto;

import android.support.annotation.NonNull;

public class CashAddr {
    static {
        System.loadLibrary("core");
    }

    private String value;

    public CashAddr(String cashAddr) {
        this.value = cashAddr;
    }

    @NonNull
    public static CashAddr fromLegacy(String legacyAddr) {
        return new CashAddr(BRCashAddrEncode(legacyAddr));
    }

    private static native String BRCashAddrDecode(String cashAddr);

    private static native String BRCashAddrEncode(String legacyAddr);

    public String toLegacy() {
        return BRCashAddrDecode(this.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
