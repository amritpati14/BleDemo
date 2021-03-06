package de.troido.bledemo.util;

import java.util.UUID;
import java.util.regex.Pattern;

public final class Uuids {
    private static final String HEX_CHARS = "0123456789abcdef";
    private static final Pattern UUID_PAT = Pattern.compile("-");

    private Uuids() {}

    private static byte[] hexStringToByteArray(String s) {
        byte[] result = new byte[s.length() / 2];
        String sLower = s.toLowerCase();
        for (int i = 0; i < s.length(); i += 2) {
            result[i >> 1] = (byte) ((HEX_CHARS.indexOf(sLower.charAt(i)) << 4)
                    | (HEX_CHARS.indexOf(sLower.charAt(i + 1))));
        }
        return result;
    }

    public static byte[] toBytes(UUID uuid) {
        return hexStringToByteArray(UUID_PAT.matcher(uuid.toString()).replaceAll(""));
    }
}
