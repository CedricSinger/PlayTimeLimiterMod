package net.c3dd1.playtimelimiter.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;

public class Utilities {

    //Round Double
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    //Converts the string from the config file to a map (PlayerID -> left playtime)
    public static HashMap<String, Double> stringToMap(String inputString) {
        String[] pairs = inputString.split(",");
        HashMap<String, Double> outputMap = new HashMap<>();
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            String[] keyValue = pair.split(":");
            outputMap.put(keyValue[0], Double.valueOf(keyValue[1]));
        }
        return outputMap;
    }

    //Converts the map containing the left playtime for each PlayerID back to a string for saving it in the config file
    public static String mapToString(HashMap<String, Double> inputMap) {
        String outputString = "";
        for(String s : inputMap.keySet()) {
            outputString = outputString.concat(s + ":" + inputMap.get(s) + ",");
        }
        return outputString.substring(0, outputString.length() - 1);
    }
}
