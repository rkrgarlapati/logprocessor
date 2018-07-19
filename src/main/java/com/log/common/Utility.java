package com.log.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class Utility {

    public static String[] getSevValues(String[] sev, String param) {

        List<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(sev));

        if(param != null) {
            for (int i = 0; i < sev.length; ) {
                if (!list.get(i).equals(param)) {
                    list.remove(i);
                } else {
                    break;
                }
            }
        }

        return list.toArray(new String[list.size()]);
    }
}
