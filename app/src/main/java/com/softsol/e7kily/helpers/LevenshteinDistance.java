package com.softsol.e7kily.helpers;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ahmed on 05-Nov-17.
 */

public class LevenshteinDistance<T> {
    private static float min(List<Float> scores) {
        float min = Float.MAX_VALUE;
        for (float f : scores) {
            if (f < min) {
                min = f;
            }
        }
        return min;
    }

    public static float GetDiffernceRate(String string1, String string2) {
        if (string1.isEmpty() || string2.isEmpty())
            return 1f;
        string1 = string1.trim().toLowerCase();
        string2 = string2.trim().toLowerCase();
        String dom = string1.length() > string2.length() ? string1 : string2;
        int result = EditDistance(string1.trim().toLowerCase(), string2.trim().toLowerCase());
        return (float) result / (float) dom.length();
    }

    public static float GetDiffernceRate_Recursive(String string1, String string2) {
        if (string1.isEmpty() || string2.isEmpty())
            return 1f;
        string1 = string1.trim().toLowerCase();
        string2 = string2.trim().toLowerCase();
        String sub = "";
        String dom = "";
        if (string1.length() > string2.length()) {
            dom = string1.trim();
            sub = string2.trim();
        } else {
            dom = string2.trim();
            sub = string1.trim();
        }

        List<Float> scores = new ArrayList<Float>();

        String tempDom = dom;

        while (tempDom.length() >= sub.length()) {
            float result = GetDiffernceRate(tempDom.substring(0, sub.length()), sub);
            scores.add(result);
            tempDom = tempDom.substring(1);
        }

        //foreach(string segment in dom.Split(" ".ToCharArray()))
        //{
        //    scores.Add(GetDiffernceRate(segment, sub));
        //}

        return min(scores);
    }

    //split with spaces rather than letter marching search
    public static float GetDiffernceRate_Recursive_02(String string1, String string2) {
        if (string1.isEmpty() || string2.isEmpty())
            return 1f;
        string1 = string1.trim().toLowerCase();
        string2 = string2.trim().toLowerCase();
        String sub = "";
        String dom = "";
        if (string1.length() > string2.length()) {
            dom = string1.trim();
            sub = string2.trim();
        } else {
            dom = string2.trim();
            sub = string1.trim();
        }

        List<Float> scores = new ArrayList<Float>();

        //string tempDom = dom;

        //while (tempDom.Length >= sub.Length)
        //{
        //    float result = GetDiffernceRate(tempDom.Substring(0, sub.Length), sub);
        //    scores.Add(result);
        //    tempDom = tempDom.Remove(0, 1);
        //}

        String[] domArr = dom.split(" ");

        for (String domWord : domArr) {
            float result = GetDiffernceRate(domWord, sub);
            scores.add(result);
        }

        //foreach(string segment in dom.Split(" ".ToCharArray()))
        //{
        //    scores.Add(GetDiffernceRate(segment, sub));
        //}

        return min(scores);
    }

    public static boolean GetDiffernceRate_Recursive_Subset(String string1, String string2, int minimumLetters, float matchThreshold) {
        if (string1.isEmpty() || string2.isEmpty())
            return false;
        string1 = string1.trim().toLowerCase();
        string2 = string2.trim().toLowerCase();
        String sub = "";
        String dom = "";
        if (string1.length() > string2.length()) {
            dom = string1.trim();
            sub = string2.trim();
        } else {
            dom = string2.trim();
            sub = string1.trim();
        }

        String[] domSep = dom.split(" ");
        String[] subSep = sub.split(" ");

        for (String domWord : domSep) {
            if (domWord.length() < minimumLetters)
                continue;

            for (String subWord : subSep) {
                if (subWord.length() < minimumLetters) {
                    continue;
                }
                if (GetDiffernceRate(domWord, subWord) < matchThreshold)
                    return true;
            }
        }
        return false;
    }

    /// <summary>
    /// Compute the distance between two strings.
    /// </summary>
    public static int EditDistance(String s, String t) {
        int n = s.length();
        int m = t.length();
        int[][] d = new int[n + 1][m + 1];

        // Step 1
        if (n == 0) {
            return m;
        }

        if (m == 0) {
            return n;
        }

        // Step 2
        for (int i = 0; i <= n; d[i][0] = i++) {
        }

        for (int j = 0; j <= m; d[0][j] = j++) {
        }

        // Step 3
        for (int i = 1; i <= n; i++) {
            //Step 4
            for (int j = 1; j <= m; j++) {
                // Step 5
                int cost = (t.charAt(j - 1) == s.charAt(i - 1)) ? 0 : 1;

                // Step 6
                d[i][j] = Math.min(
                        Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1),
                        d[i - 1][j - 1] + cost);
            }
        }
        // Step 7
        return d[n][m];
    }
}
