package com.log.common;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexMatch implements Serializable {
    final static String timestampRgx = "(?<timestamp>\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.\\d{3})";
    final static String thread1 = "(?<thread1>\\d{0,})";
    final static String thread2 = "(?<thread2>\\d{0,})";
    final static String sevRgx = "(?<sev>I|E|W|T|D|F|V)";
    final static String tagRgx = "(?<tag>.*?\\:)";
    final static String messageRgx = "(?<message>[^*]*)";
    public static Pattern PatternFullLog = Pattern.compile(timestampRgx + " +" + thread1 + " +" + thread2 + " +" + sevRgx + " +" + tagRgx + " +" + messageRgx, Pattern.DOTALL);

    public static void main(String[] args) {
        RegexMatch match = new RegexMatch();
        String test = "10-15 14:22:45.583 18738 18750 D UsbRequestJNI: close";
        System.out.println(match.isMatched(test));
        String spacestest = "03-27 18:09:31.407  6854  7484 D UsbRequestJNI: init";
        System.out.println(match.isMatched(spacestest));
    }

    public boolean isMatched(String data) {

        Matcher matcher = PatternFullLog.matcher(data);
        //matcher.find();
        /*if(matcher.find()) {
            System.out.println(matcher.group("timestamp"));
            System.out.println(matcher.group("tag"));
            System.out.println(matcher.group("sev"));
            System.out.println(matcher.group("message"));

            System.out.println("----");
        }*/

        return matcher.find();
    }
}