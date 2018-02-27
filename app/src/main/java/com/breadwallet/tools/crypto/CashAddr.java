package com.breadwallet.tools.crypto;

import android.support.annotation.NonNull;

import com.breadwallet.BuildConfig;

public class CashAddr {

    private static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";

    static {
        System.loadLibrary("core");
    }

    private String value;
    private byte[] hash;

    private CashAddr(byte[] hash) {

    }

    private CashAddr(String cashAddr) {
        this.value = cashAddr;
    }

    @NonNull
    public static CashAddr fromLegacy(String legacyAddr) {
        return new CashAddr(BRCashAddrEncode(legacyAddr));
    }

    public static CashAddr parse(String cashAddr) {
        if (!(cashAddr.equals(cashAddr.toLowerCase()) || cashAddr.equals(cashAddr.toUpperCase()))) {
            throw new IllegalArgumentException("cannot_mix_case");
        }

        final String[] parts = cashAddr.split(":");
        if (parts.length != 2 || !parts[0].equals(BuildConfig.CASHADDR_PREFIX)) {
            throw new IllegalArgumentException("invalid_prefix");
        }

        byte[] buffer = cashAddr.getBytes();
        for (byte b : buffer) {
            if (b < 0x21 || b > 0x7e) {
                throw new IllegalArgumentException("illegal_char");
            }
        }

        //TODO: checksum

        return new CashAddr(cashAddr);
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

    public String  toUnprefixedString(){
        return value.replace(BuildConfig.CASHADDR_PREFIX + ":", "");
    }

    private int polymod(byte[] values) {

        final int[] GENERATORS = {0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3};

        int chk = 1;

        for (byte b : values) {
            byte top = (byte) (chk >> 0x19);
            chk = b ^ ((chk & 0x1ffffff) << 5);
            for (int i = 0; i < 5; i++) {
                chk ^= ((top >> i) & 1) == 1 ? GENERATORS[i] : 0;
            }
        }

        return chk;
    }
}
