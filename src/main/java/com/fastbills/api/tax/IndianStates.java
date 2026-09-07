package com.fastbills.api.tax;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class IndianStates {

    private static final Map<String, String> CODE_TO_NAME;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("01", "Jammu and Kashmir");
        map.put("02", "Himachal Pradesh");
        map.put("03", "Punjab");
        map.put("04", "Chandigarh");
        map.put("05", "Uttarakhand");
        map.put("06", "Haryana");
        map.put("07", "Delhi");
        map.put("08", "Rajasthan");
        map.put("09", "Uttar Pradesh");
        map.put("10", "Bihar");
        map.put("11", "Sikkim");
        map.put("12", "Arunachal Pradesh");
        map.put("13", "Nagaland");
        map.put("14", "Manipur");
        map.put("15", "Mizoram");
        map.put("16", "Tripura");
        map.put("17", "Meghalaya");
        map.put("18", "Assam");
        map.put("19", "West Bengal");
        map.put("20", "Jharkhand");
        map.put("21", "Odisha");
        map.put("22", "Chhattisgarh");
        map.put("23", "Madhya Pradesh");
        map.put("24", "Gujarat");
        map.put("25", "Daman and Diu");
        map.put("26", "Dadra and Nagar Haveli");
        map.put("27", "Maharashtra");
        map.put("28", "Andhra Pradesh");
        map.put("29", "Karnataka");
        map.put("30", "Goa");
        map.put("31", "Lakshadweep");
        map.put("32", "Kerala");
        map.put("33", "Tamil Nadu");
        map.put("34", "Puducherry");
        map.put("35", "Andaman and Nicobar Islands");
        map.put("36", "Telangana");
        map.put("37", "Andhra Pradesh (New)");
        map.put("38", "Ladakh");
        map.put("97", "Other Territory");
        CODE_TO_NAME = Collections.unmodifiableMap(map);
    }

    private IndianStates() {}

    public static String getStateName(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return CODE_TO_NAME.getOrDefault(code.trim(), "State (" + code + ")");
    }
}
