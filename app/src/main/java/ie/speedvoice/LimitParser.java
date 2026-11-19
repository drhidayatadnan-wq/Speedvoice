package ie.speedvoice;
import java.util.Locale;
import java.util.regex.*;
final class LimitParser {
 private static final Pattern LABEL=Pattern.compile("(?i)\\b(?:road\\s+)?speed\\s+limit\\s*[:=,]?\\s*(\\d{1,3})\\s*(km/h|kmh|kph|kilomet(?:er|re)s?(?: per hour)?|mph|miles? per hour)?\\s*[.!]?\\s*$");
 private static final Pattern REVERSE=Pattern.compile("(?i)^\\s*(\\d{1,3})\\s*(km/h|kmh|kph|kilomet(?:er|re)s?(?: per hour)?)\\s*[,–-]?\\s*(?:road\\s+)?speed\\s+limit\\s*[.!]?\\s*$");
 static int parse(String value){
 if(value==null)return -1;
 String s=value.trim().replace('\u00a0',' ');
 Matcher m=LABEL.matcher(s);
 if(!m.matches()){m=REVERSE.matcher(s);if(!m.matches())return -1;}
 String unit=m.group(2);
 if(unit!=null&&unit.toLowerCase(Locale.ROOT).startsWith("m"))return -1;
 int n=Integer.parseInt(m.group(1));
 return n>=10&&n<=130&&n%5==0?n:-1;
 }
}