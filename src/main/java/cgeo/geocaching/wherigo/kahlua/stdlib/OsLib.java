/*
Copyright(c) 2008 Kevin Lundberg <kevinrlundberg@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files(the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
--
--
File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package se.krka.kahlua.stdlib
*/

package cgeo.geocaching.wherigo.kahlua.stdlib;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;

public enum OsLib implements JavaFunction {
    DATE("date"),
    DIFFTIME("difftime"),
    TIME("time");

    private static final String TABLE_FORMAT = "*t";
    private static final String DEFAULT_FORMAT = "%c";

    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DAY = "day";
    private static final String HOUR = "hour";
    private static final String MIN = "min";
    private static final String SEC = "sec";
    private static final String WDAY = "wday";
    private static final String YDAY = "yday";
    private static final Object MILLISECOND = "milli";
    //private static final String ISDST = "isdst";

    private static ZoneId tzone = ZoneId.systemDefault();

    public static final int TIME_DIVIDEND = 1000; // number to divide by for converting from milliseconds.
    public static final double TIME_DIVIDEND_INVERTED = 1.0 / TIME_DIVIDEND; // number to divide by for converting from milliseconds.
    private static final int MILLIS_PER_DAY = TIME_DIVIDEND * 60 * 60 * 24;
    private static final int MILLIS_PER_WEEK = MILLIS_PER_DAY * 7;

    private final String name;

    private static String[] shortDayNames = new String[] {
        "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };

    private static String[] longDayNames = new String[] {
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    private static String[] shortMonthNames = new String[] {
        "Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"
    };

    private static String[] longMonthNames = new String[] {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    OsLib(String name) {
        this.name = name;
    }

    public static void register(LuaState state) {
        LuaTable os = new LuaTableImpl();
        state.getEnvironment().rawset("os", os);

        for (OsLib func : OsLib.values()) {
            os.rawset(func.name, func);
        }
    }

    public static void setTimeZone (ZoneId tz) {
        tzone = tz;
    }

    @Override
    public int call(LuaCallFrame cf, int nargs) {
        return switch (this) {
            case DATE -> date(cf, nargs);
            case DIFFTIME -> difftime(cf);
            case TIME -> time(cf, nargs);
        };
    }

    static int time(LuaCallFrame cf, int nargs) {
        if (nargs == 0) {
            double t = (double) System.currentTimeMillis() * TIME_DIVIDEND_INVERTED;
            cf.push(LuaState.toDouble(t));
        } else {
            LuaTable table = (LuaTable) BaseLib.getArg(cf, 1, BaseLib.TYPE_TABLE, "time");
            double t = (double) getInstantFromTable(table).toEpochMilli() * TIME_DIVIDEND_INVERTED;
            cf.push(LuaState.toDouble(t));
        }
        return 1;
    }

    static int difftime(LuaCallFrame cf) {
        double t2 = BaseLib.rawTonumber(cf.get(0)).doubleValue();
        double t1 = BaseLib.rawTonumber(cf.get(1)).doubleValue();
        cf.push(LuaState.toDouble(t2-t1));
        return 1;
    }

    static int date(LuaCallFrame cf, int nargs) {
        if (nargs == 0) {
            return cf.push(getdate(DEFAULT_FORMAT));
        } else {
            String format = BaseLib.rawTostring(cf.get(0));
            if (nargs == 1) {
                return cf.push(getdate(format));
            } else {
                Double rawTonumber = BaseLib.rawTonumber(cf.get(1));
                long time = (long) (rawTonumber.doubleValue() * TIME_DIVIDEND);
                return cf.push(getdate(format, time));
            }
        }
    }

    public static Object getdate(String format) {
        return getdate(format, Instant.now().toEpochMilli());
    }

    public static Object getdate(String format, long time) {
        //boolean universalTime = format.startsWith("!");
        ZonedDateTime dateTime = null;
        int si = 0;
        if (format.charAt(si) == '!') { // UTC?
            dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.of("UTC"));
            si++;  // skip '!'
        } else {
            dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), tzone);
        }

        if (dateTime == null) { // invalid dateTime?
            return null;
        } else if (format.substring(si, 2 + si).equals(TABLE_FORMAT)) {
            return getTableFromDate(dateTime);
        } else {
            return formatTime(format.substring(si), dateTime);
        }
    }

    public static String formatTime(String format, ZonedDateTime dt) {

        StringBuffer buffer = new StringBuffer();
        for (int stringIndex = 0; stringIndex < format.length(); stringIndex ++) {
            if (format.charAt(stringIndex) != '%' || stringIndex + 1 == format.length()) { // no conversion specifier?
                buffer.append(format.charAt(stringIndex));
            } else {
                ++stringIndex;
                buffer.append(strftime(format.charAt(stringIndex), dt));
            }
        }
        return buffer.toString();
    }

    private static String strftime(char format, ZonedDateTime dt) {
        return switch (format) {
            case 'a' -> shortDayNames[dt.getDayOfWeek().getValue() % 7];
            case 'A' -> longDayNames[dt.getDayOfWeek().getValue() % 7];
            case 'b' -> shortMonthNames[dt.getMonthValue() - 1];
            case 'B' -> longMonthNames[dt.getMonthValue() - 1];
            case 'c' -> dt.toString();
            case 'C' -> Integer.toString(dt.getYear() / 100);
            case 'd' -> Integer.toString(dt.getDayOfMonth());
            case 'D' -> formatTime("%m/%d/%y", dt);
            case 'e' -> dt.getDayOfMonth() < 10 ?
                    " " + strftime('d', dt) : strftime('d', dt);
            case 'h' -> strftime('b', dt);
            case 'H' -> Integer.toString(dt.getHour());
            case 'I' -> Integer.toString(dt.getHour() % 12 == 0 ? 12 : dt.getHour() % 12);
            case 'j' -> Integer.toString(dt.getDayOfYear());
            case 'm' -> Integer.toString(dt.getMonthValue());
            case 'M' -> Integer.toString(dt.getMinute());
            case 'n' -> "\n";
            case 'p' -> dt.getHour() < 12 ? "AM" : "PM";
            case 'r' -> formatTime("%I:%M:%S %p", dt);
            case 'R' -> formatTime("%H:%M", dt);
            case 'S' -> Integer.toString(dt.getSecond());
            case 'U' -> Integer.toString(getWeekOfYear(dt, true, false));
            case 'V' -> Integer.toString(getWeekOfYear(dt, false, true));
            case 'w' -> Integer.toString(dt.getDayOfWeek().getValue() % 7);
            case 'W' -> Integer.toString(getWeekOfYear(dt, false, false));
            /* commented out until we have a way to define locale and get locale formats working
            case 'x':
                String str = Integer.toString(dt.getYear());
                return Integer.toString(dt.getMonthValue()) + "/" + Integer.toString(dt.getDayOfMonth()) +
                        "/" + str.substring(2, str.length());
            case 'X': return Integer.toString(dt.getHour()) + ":" + Integer.toString(dt.getMinute()) +
                        ":" + Integer.toString(dt.getSecond());
            */
            case 'y' -> Integer.toString(dt.getYear() % 100);
            case 'Y' -> Integer.toString(dt.getYear());
            case 'Z' -> dt.getZone().getId();
            default -> null; // bad input format.
        };
    }

    public static LuaTable getTableFromDate(ZonedDateTime dt) {
        LuaTable time = new LuaTableImpl();
        time.rawset(YEAR, LuaState.toDouble(dt.getYear()));
        time.rawset(MONTH, LuaState.toDouble(dt.getMonthValue()));
        time.rawset(DAY, LuaState.toDouble(dt.getDayOfMonth()));
        time.rawset(HOUR, LuaState.toDouble(dt.getHour()));
        time.rawset(MIN, LuaState.toDouble(dt.getMinute()));
        time.rawset(SEC, LuaState.toDouble(dt.getSecond()));
        time.rawset(WDAY, LuaState.toDouble(dt.getDayOfWeek().getValue() % 7 + 1));
        time.rawset(YDAY, LuaState.toDouble(dt.getDayOfYear()));
        time.rawset(MILLISECOND, LuaState.toDouble(dt.get(ChronoField.MILLI_OF_SECOND)));
        //time.rawset(ISDST, null);
        return time;
    }

    /**
     * converts the relevant fields in the given luatable to an Instant object.
     * @param time LuaTable with entries for year month and day, and optionally hour/min/sec
     * @return an Instant object representing the date from the luatable.
     */
    public static Instant getInstantFromTable(LuaTable time) {
        int year = (int)LuaState.fromDouble(time.rawget(YEAR));
        int month = (int)LuaState.fromDouble(time.rawget(MONTH));
        int day = (int)LuaState.fromDouble(time.rawget(DAY));
        
        Object hour = time.rawget(HOUR);
        Object minute = time.rawget(MIN);
        Object seconds = time.rawget(SEC);
        Object milliseconds = time.rawget(MILLISECOND);
        //Object isDst = time.rawget(ISDST);
        
        int hourVal = hour != null ? (int)LuaState.fromDouble(hour) : 0;
        int minuteVal = minute != null ? (int)LuaState.fromDouble(minute) : 0;
        int secondVal = seconds != null ? (int)LuaState.fromDouble(seconds) : 0;
        int milliVal = milliseconds != null ? (int)LuaState.fromDouble(milliseconds) : 0;
        
        LocalDateTime ldt = LocalDateTime.of(year, month, day, hourVal, minuteVal, secondVal, milliVal * 1_000_000);
        ZonedDateTime zdt = ZonedDateTime.of(ldt, tzone);
        // TODO: daylight savings support(is it possible?)
        return zdt.toInstant();
    }

    public static int getWeekOfYear(ZonedDateTime dt, boolean weekStartsSunday, boolean jan1midweek) {
        ZonedDateTime startOfYear = dt.withDayOfYear(1);
        int dayOfWeek = startOfYear.getDayOfWeek().getValue(); // Monday=1, Sunday=7
        
        if (weekStartsSunday && dayOfWeek != 7) { // Sunday is 7 in ISO
            startOfYear = startOfYear.plusDays(7 - dayOfWeek + 1);
        } else if (!weekStartsSunday && dayOfWeek != 1) { // Monday is 1 in ISO
            startOfYear = startOfYear.plusDays(7 - dayOfWeek + 1 + 1);
        }
        
        long diff = ChronoUnit.MILLIS.between(startOfYear, dt);
        int w = (int)(diff / MILLIS_PER_WEEK);

        if (jan1midweek && 7-dayOfWeek >= 4)
            w++;

        return w;
    }
}
