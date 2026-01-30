/*
Copyright (c) 2007 - 2009 Kristofer Karlsson <kristofer.karlsson@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
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
*/
package cgeo.geocaching.wherigo.openwig.kahlua.stdlib;

import cgeo.geocaching.wherigo.openwig.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.openwig.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.openwig.kahlua.vm.LuaClosure;
import cgeo.geocaching.wherigo.openwig.kahlua.vm.LuaException;
import cgeo.geocaching.wherigo.openwig.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.openwig.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.openwig.kahlua.vm.LuaThread;

public enum BaseLib implements JavaFunction {
    PCALL,
    PRINT,
    SELECT,
    TYPE,
    TOSTRING,
    TONUMBER,
    GETMETATABLE,
    SETMETATABLE,
    ERROR,
    UNPACK,
    NEXT,
    SETFENV,
    GETFENV,
    RAWEQUAL,
    RAWSET,
    RAWGET,
    COLLECTGARBAGE,
    DEBUGSTACKTRACE,
    BYTECODELOADER;

    public enum Type {
        NIL("nil"),
        STRING("string"),
        NUMBER("number"),
        BOOLEAN("boolean"),
        FUNCTION("function"),
        TABLE("table"),
        THREAD("thread"),
        USERDATA("userdata");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private static final Runtime RUNTIME = Runtime.getRuntime();
    public static final Object MODE_KEY = "__mode";
    private static final Object DOUBLE_ONE = new Double(1.0);

    public static void register(LuaState state) {
        for (BaseLib f : values()) {
            state.getEnvironment().rawset(f.name().toLowerCase(), f);
        }
    }

    public String toString() {
        return name().toLowerCase();
    }


    public int call(LuaCallFrame callFrame, int nArguments) {
        switch (this) {
        case PCALL: return pcall(callFrame, nArguments);
        case PRINT: return print(callFrame, nArguments);
        case SELECT: return select(callFrame, nArguments);
        case TYPE: return type(callFrame, nArguments);
        case TOSTRING: return tostring(callFrame, nArguments);
        case TONUMBER: return tonumber(callFrame, nArguments);
        case GETMETATABLE: return getmetatable(callFrame, nArguments);
        case SETMETATABLE: return setmetatable(callFrame, nArguments);
        case ERROR: return error(callFrame, nArguments);
        case UNPACK: return unpack(callFrame, nArguments);
        case NEXT: return next(callFrame, nArguments);
        case SETFENV: return setfenv(callFrame, nArguments);
        case GETFENV: return getfenv(callFrame, nArguments);
        case RAWEQUAL: return rawequal(callFrame, nArguments);
        case RAWSET: return rawset(callFrame, nArguments);
        case RAWGET: return rawget(callFrame, nArguments);
        case COLLECTGARBAGE: return collectgarbage(callFrame, nArguments);
        case DEBUGSTACKTRACE: return debugstacktrace(callFrame, nArguments);
        case BYTECODELOADER: return bytecodeloader(callFrame, nArguments);
        default:
            // Should never happen
            // throw new Error("Illegal function object");
            return 0;
        }
    }
    
    private int debugstacktrace(LuaCallFrame callFrame, int nArguments) {
        LuaThread thread = (LuaThread) getOptArg(callFrame, 1, BaseLib.Type.THREAD.toString());
        if (thread == null) {
            thread = callFrame.thread;
        }
        Double levelDouble = (Double) getOptArg(callFrame, 2, BaseLib.Type.NUMBER.toString());
        int level = 0;
        if (levelDouble != null) {
            level = levelDouble.intValue();
        }
        Double countDouble = (Double) getOptArg(callFrame, 3, BaseLib.Type.NUMBER.toString());
        int count = Integer.MAX_VALUE;
        if (countDouble != null) {
            count = countDouble.intValue(); 
        }
        Double haltAtDouble = (Double) getOptArg(callFrame, 4, BaseLib.Type.NUMBER.toString());
        int haltAt = 0;
        if (haltAtDouble != null) {
            haltAt = haltAtDouble.intValue(); 
        }
        return callFrame.push(thread.getCurrentStackTrace(level, count, haltAt));
    }

    private int rawget(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 2, "Not enough arguments");
        LuaTable t = (LuaTable) callFrame.get(0);
        Object key = callFrame.get(1);

        callFrame.push(t.rawget(key));
        return 1;
    }

    private int rawset(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 3, "Not enough arguments");
        LuaTable t = (LuaTable) callFrame.get(0);
        Object key = callFrame.get(1);
        Object value = callFrame.get(2);

        t.rawset(key, value);
        callFrame.setTop(1);
        return 1;
    }

    private int rawequal(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 2, "Not enough arguments");
        Object o1 = callFrame.get(0);
        Object o2 = callFrame.get(1);

        callFrame.push(toBoolean(LuaState.luaEquals(o1, o2)));
        return 1;
    }

    private static final Boolean toBoolean(boolean b) {
        if (b) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private int setfenv(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 2, "Not enough arguments");

        LuaTable newEnv = (LuaTable) callFrame.get(1);
        luaAssert(newEnv != null, "expected a table");
        
        LuaClosure closure = null;
        
        Object o = callFrame.get(0);
        if (o instanceof LuaClosure) {
            closure = (LuaClosure) o;
        } else {
            o = rawTonumber(o);
            luaAssert(o != null, "expected a lua function or a number");
            int level = ((Double) o).intValue();
            if (level == 0) {
                callFrame.thread.environment = newEnv;
                return 0;
            }
            LuaCallFrame parentCallFrame = callFrame.thread.getParent(level);
            if (!parentCallFrame.isLua()) {
                fail("No closure found at this level: " + level);
            }
            closure = parentCallFrame.closure;
        }

        closure.env = newEnv;

        callFrame.setTop(1);
        return 1;
    }

    private int getfenv(LuaCallFrame callFrame, int nArguments) {
        Object o = DOUBLE_ONE;
        if (nArguments >= 1) {
            o = callFrame.get(0);
        }

        Object res = null;
        if (o == null || o instanceof JavaFunction) {
            res = callFrame.thread.environment;
        } else if (o instanceof LuaClosure) {
            LuaClosure closure = (LuaClosure) o;
            res = closure.env;
        } else {
            Double d = rawTonumber(o);
            luaAssert(d != null, "Expected number");
            int level = d.intValue();
            luaAssert(level >= 0, "level must be non - negative");
            LuaCallFrame callFrame2 = callFrame.thread.getParent(level);
            res = callFrame2.getEnvironment();
        }
        callFrame.push(res);
        return 1;
    }

    private int next(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments");

        LuaTable t = (LuaTable) callFrame.get(0);
        Object key = null;

        if (nArguments >= 2) {
            key = callFrame.get(1);
        }

        Object nextKey = t.next(key);
        if (nextKey == null) {
            callFrame.setTop(1);
            callFrame.set(0, null);
            return 1;
        }

        Object value = t.rawget(nextKey);

        callFrame.setTop(2);
        callFrame.set(0, nextKey);
        callFrame.set(1, value);
        return 2;
    }

    private int unpack(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments");

        LuaTable t = (LuaTable) callFrame.get(0);

        Object di = null, dj = null;
        if (nArguments >= 2) {
            di = callFrame.get(1);
        }
        if (nArguments >= 3) {
            dj = callFrame.get(2);
        }

        int i, j;
        if (di != null) {
            i = (int) LuaState.fromDouble(di);
        } else {
            i = 1;
        }

        if (dj != null) {
            j = (int) LuaState.fromDouble(dj);
        } else {
            j = t.len();
        }

        int nReturnValues = 1 + j - i;

        if (nReturnValues <= 0) {
            callFrame.setTop(0);
            return 0;
        }

        callFrame.setTop(nReturnValues);
        for (int b = 0; b < nReturnValues; b++) {
            callFrame.set(b, t.rawget(LuaState.toDouble((i + b))));
        }
        return nReturnValues;
    }

    private int error(LuaCallFrame callFrame, int nArguments) {
        if (nArguments >= 1) {
            String stacktrace = (String) getOptArg(callFrame, 2, BaseLib.Type.STRING.toString());
            if (stacktrace == null) {
                stacktrace = "";
            }
            callFrame.thread.stackTrace = stacktrace;
            throw new LuaException(callFrame.get(0));
        }
        return 0;
    }

    public static int pcall(LuaCallFrame callFrame, int nArguments) {
        return callFrame.thread.state.pcall(nArguments - 1);
    }

    private static int print(LuaCallFrame callFrame, int nArguments) {
        LuaState state = callFrame.thread.state;
        LuaTable env = state.getEnvironment();
        Object toStringFun = state.tableGet(env, "tostring");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < nArguments; i++) {
            if (i > 0) {
                sb.append("\t");
            }

            Object res = state.call(toStringFun, callFrame.get(i), null, null);

            sb.append(res);
        }
        state.getOut().println(sb.toString());
        return 0;
    }

    private static int select(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments");
        Object arg1 = callFrame.get(0);
        if (arg1 instanceof String) {
            if (((String) arg1).startsWith("#")) {
                callFrame.push(LuaState.toDouble(nArguments - 1));
                return 1;
            }
        }
        Double dIndexDouble = rawTonumber(arg1);
        double dIndex = LuaState.fromDouble(dIndexDouble);
        int index = (int) dIndex;
        if (index >= 1 && index <= (nArguments - 1)) {
            int nResults = nArguments - index;
            return nResults;
        }
        return 0;
    }

    public static void luaAssert(boolean b, String msg) {
        if (!b) {
            fail(msg);
        }
    }

    public static void fail(String msg) {
        throw new RuntimeException(msg);
    }

    public static String numberToString(Double num) {
        if (num.isNaN()) {
            return "nan";
        }
        if (num.isInfinite()) {
            if (MathLib.isNegative(num.doubleValue())) {
                return "-inf";
            }
            return "inf";
        }
        double n = num.doubleValue();
        if (Math.floor(n) == n && Math.abs(n) < 1e14) {
            return String.valueOf(num.longValue());
        }
        return num.toString();
    }

    /**
     * 
     * @param callFrame
     * @param n
     * @param type must be "string" or "number" or one of the other built in types. Note that this parameter must be interned!
     * It's not valid to call it with new String("number").  Use null if you don't care which type or expect 
     * more than one type for this argument.
     * @param function name of the function that calls this. Only for pretty exceptions.
     * @return variable with index n on the stack, returned as type "type".
     */
    public static Object getArg(LuaCallFrame callFrame, int n, String type,
                String function) {
        Object o = callFrame.get(n - 1);
        if (o == null) {
            throw new RuntimeException("bad argument #" + n + "to '" + function +
                "' (" + type + " expected, got no value)");
        }
        // type coercion
        if (type == Type.STRING.toString()) {
            String res = rawTostring(o);
            if (res != null) {
                return res;
            }
        } else if (type == Type.NUMBER.toString()) {
            Double d = rawTonumber(o);
            if (d != null) {
                return d;
            }
            throw new RuntimeException("bad argument #" + n + " to '" + function +
            "' (number expected, got string)");
        }
        if (type != null) {
            // type checking
            String isType = type(o);
            if (type != isType) {
                fail("bad argument #" + n + " to '" + function +"' (" + type +
                    " expected, got " + isType + ")");
            }
        }
        return o;

    }

    public static Object getOptArg(LuaCallFrame callFrame, int n, String type) {
        // Outside of stack
        if (n - 1 >= callFrame.getTop()) {
            return null;
        }
        
        Object o = callFrame.get(n - 1);
        if (o == null) {
            return null;
        }
        // type coercion
        if (type == Type.STRING.toString()) {
            return rawTostring(o);
        } else if (type == Type.NUMBER.toString()) {
            return rawTonumber(o);
        }
        // no type checking, this is optional after all
        return o;
    }

    private static int getmetatable(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments");
        Object o = callFrame.get(0);

        Object metatable = callFrame.thread.state.getmetatable(o, false);
        callFrame.push(metatable);
        return 1;
    }

    private static int setmetatable(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 2, "Not enough arguments");

        Object o = callFrame.get(0);

        LuaTable newMeta = (LuaTable) (callFrame.get(1));
        setmetatable(callFrame.thread.state, o, newMeta, false);

        callFrame.setTop(1);
        return 1;
    }

    public static void setmetatable(LuaState state, Object o, LuaTable newMeta, boolean raw) {
        luaAssert(o != null, "Expected table, got nil");
        final Object oldMeta = state.getmetatable(o, raw);

        if (!raw && oldMeta != null && state.tableGet(oldMeta, "__metatable") != null) {
            throw new RuntimeException("Can not set metatable of protected object");
        }

        state.setmetatable(o, newMeta);
    }

    private static int type(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments");
        Object o = callFrame.get(0);
        callFrame.push(type(o));
        return 1;
    }

    public static String type(Object o) {
        if (o == null) {
            return Type.NIL.toString();
        }
        if (o instanceof String) {
            return Type.STRING.toString();
        }
        if (o instanceof Double) {
            return Type.NUMBER.toString();
        }
        if (o instanceof Boolean) {
            return Type.BOOLEAN.toString();
        }
        if (o instanceof JavaFunction || o instanceof LuaClosure) {
            return Type.FUNCTION.toString();
        }
        if (o instanceof LuaTable) {
            return Type.TABLE.toString();
        }
        if (o instanceof LuaThread) {
            return Type.THREAD.toString();
        }
        return Type.USERDATA.toString();
    }

    private static int tostring(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments");
        Object o = callFrame.get(0);
        Object res = tostring(o, callFrame.thread.state);
        callFrame.push(res);
        return 1;
    }

    public static String tostring(Object o, LuaState state) {
        if (o == null) {
            return Type.NIL.toString();
        }
        if (o instanceof String) {
            return (String) o;
        }
        if (o instanceof Double) {
            return rawTostring(o);
        }
        if (o instanceof Boolean) {
            return o == Boolean.TRUE ? "true" : "false";
        }
        if (o instanceof JavaFunction) {
            return "function 0x" + System.identityHashCode(o);
        }
        if (o instanceof LuaClosure) {
            return "function 0x" + System.identityHashCode(o);
        }

        Object tostringFun = state.getMetaOp(o, "__tostring");
        if (tostringFun != null) {
            String res = (String) state.call(tostringFun, o, null, null);

            return res;
        }

        if (o instanceof LuaTable) {
            return "table 0x" + System.identityHashCode(o);
        }
        throw new RuntimeException("no __tostring found on object");
    }

    private static int tonumber(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments");
        Object o = callFrame.get(0);

        if (nArguments == 1) {
            callFrame.push(rawTonumber(o));
            return 1;
        }

        String s = (String) o;

        Object radixObj = callFrame.get(1);
        Double radixDouble = rawTonumber(radixObj);
        luaAssert(radixDouble != null, "Argument 2 must be a number");

        double dradix = LuaState.fromDouble(radixDouble);
        int radix = (int) dradix;
        if (radix != dradix) {
            throw new RuntimeException("base is not an integer");
        }
        Object res = tonumber(s, radix);
        callFrame.push(res);
        return 1;
    }

    public static Double tonumber(String s) {
        return tonumber(s, 10);
    }

    public static Double tonumber(String s, int radix)  {
        if (radix < 2 || radix > 36) {
            throw new RuntimeException("base out of range");
        }

        try {
            if (radix == 10) {
                return Double.valueOf(s);
            } else {
                return LuaState.toDouble(Integer.parseInt(s, radix));
            }
        } catch (NumberFormatException e) {
            s = s.toLowerCase();
            if (s.endsWith("nan")) {
                return LuaState.toDouble(Double.NaN);
            }
            if (s.endsWith("inf")) {
                if (s.charAt(0) == '-') {
                    return LuaState.toDouble(Double.NEGATIVE_INFINITY);
                }
                return LuaState.toDouble(Double.POSITIVE_INFINITY);
            }
            return null;
        }
    }

    public static int collectgarbage(LuaCallFrame callFrame, int nArguments) {
        Object option = null;
        if (nArguments > 0) {
            option = callFrame.get(0);
        }

        if (option == null || option.equals("step") || option.equals("collect")) {
            System.gc();
            return 0;
        }

        if (option.equals("count")) {
            long freeMemory = RUNTIME.freeMemory();
            long totalMemory = RUNTIME.totalMemory();
            callFrame.setTop(3);
            callFrame.set(0, toKiloBytes(totalMemory - freeMemory));
            callFrame.set(1, toKiloBytes(freeMemory));
            callFrame.set(2, toKiloBytes(totalMemory));
            return 3;
        }
        throw new RuntimeException("invalid option: " + option);
    }

    private static Double toKiloBytes(long freeMemory) {
        return LuaState.toDouble((freeMemory) / 1024.0);
    }

    public static String rawTostring(Object o) {
        if (o instanceof String) {
            return (String) o;
        }
        if (o instanceof Double) {
            return numberToString((Double) o);
        }
        return null;
    }

    public static Double rawTonumber(Object o) {
        if (o instanceof Double) {
            return (Double) o;
        }
        if (o instanceof String) {
            return tonumber((String) o);
        }
        return null;
    }

    private static int bytecodeloader(LuaCallFrame callFrame, int nArguments) {
        String modname = (String) getArg(callFrame, 1, "string", "loader");

        LuaTable packageTable = (LuaTable) callFrame.getEnvironment().rawget("package");
        String classpath = (String) packageTable.rawget("classpath");
        
        int index = 0;
        while (index < classpath.length()) {
            int nextIndex = classpath.indexOf(";", index);

            if (nextIndex == -1) {
                nextIndex = classpath.length();
            }
            
            String path = classpath.substring(index, nextIndex);
            if (path.length() > 0) {
                if (!path.endsWith("/")) {
                    path = path + "/";
                }
                LuaClosure closure = callFrame.thread.state.loadByteCodeFromResource(path + modname, callFrame.getEnvironment());
                if (closure != null) {
                    return callFrame.push(closure);
                }
            }
            index = nextIndex;
        }
        return callFrame.push("Could not find the bytecode for '" + modname + "' in classpath");
    }

    
}
