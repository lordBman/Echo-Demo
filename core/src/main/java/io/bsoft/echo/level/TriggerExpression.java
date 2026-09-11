package io.bsoft.echo.level;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import io.bsoft.echo.objects.TriggerSource;
import io.bsoft.echo.objects.Triggers;

/**
 * Parses the small trigger language used in level data into {@link TriggerSource}s:
 * <pre>
 *   plate1                  reference by object id
 *   all(a, b)               every source active
 *   any(a, b)               any source active
 *   not(a)
 *   timed(a, 3)             active 3 s after a's rising edge (re-triggerable)
 *   cycle(period, on, off)  free-running cycle; third argument = phase offset
 *   always / never
 * </pre>
 * Stateful triggers created here (timed, cycle) are collected so the level can update and
 * reset them.
 */
public final class TriggerExpression {

    private final ObjectMap<String, TriggerSource> sources;
    private final Array<Object> stateful;

    public TriggerExpression(ObjectMap<String, TriggerSource> sources, Array<Object> stateful) {
        this.sources = sources;
        this.stateful = stateful;
    }

    public TriggerSource parse(String expression, String context) {
        if (expression == null) {
            return null;
        }
        String expr = expression.trim();
        if (expr.isEmpty()) {
            return null;
        }
        int open = expr.indexOf('(');
        if (open < 0) {
            return atom(expr, context);
        }
        if (!expr.endsWith(")")) {
            throw new LevelLoadException(context + ": malformed trigger '" + expression + "'");
        }
        String fn = expr.substring(0, open).trim().toLowerCase();
        Array<String> args = splitArgs(expr.substring(open + 1, expr.length() - 1));
        switch (fn) {
            case "all" -> {
                return Triggers.allOf(parseAll(args, context));
            }
            case "any" -> {
                return Triggers.anyOf(parseAll(args, context));
            }
            case "not" -> {
                requireArgs(args, 1, fn, context);
                return Triggers.not(parse(args.get(0), context));
            }
            case "timed" -> {
                requireArgs(args, 2, fn, context);
                Triggers.Timed timed = new Triggers.Timed(parse(args.get(0), context),
                        number(args.get(1), context), true);
                stateful.add(timed);
                return timed;
            }
            case "cycle" -> {
                if (args.size < 2) {
                    throw new LevelLoadException(context + ": cycle(period, onTime[, offset]) needs 2 or 3 arguments");
                }
                float offset = args.size > 2 ? number(args.get(2), context) : 0f;
                Triggers.Cycle cycle = new Triggers.Cycle(number(args.get(0), context), number(args.get(1), context),
                        offset);
                stateful.add(cycle);
                return cycle;
            }
            case "latch" -> {
                requireArgs(args, 1, fn, context);
                Triggers.Latch latch = new Triggers.Latch(parse(args.get(0), context));
                stateful.add(latch);
                return latch;
            }
            default -> throw new LevelLoadException(context + ": unknown trigger function '" + fn + "'");
        }
    }

    private TriggerSource atom(String name, String context) {
        if (name.equalsIgnoreCase("always")) {
            return TriggerSource.ALWAYS;
        }
        if (name.equalsIgnoreCase("never")) {
            return TriggerSource.NEVER;
        }
        TriggerSource source = sources.get(name);
        if (source == null) {
            throw new LevelLoadException(context + ": trigger references unknown object '" + name + "'");
        }
        return source;
    }

    private TriggerSource[] parseAll(Array<String> args, String context) {
        TriggerSource[] result = new TriggerSource[args.size];
        for (int i = 0; i < args.size; i++) {
            result[i] = parse(args.get(i), context);
        }
        return result;
    }

    private static void requireArgs(Array<String> args, int n, String fn, String context) {
        if (args.size != n) {
            throw new LevelLoadException(context + ": " + fn + "() expects " + n + " argument(s), got " + args.size);
        }
    }

    private static float number(String s, String context) {
        try {
            return Float.parseFloat(s.trim());
        } catch (NumberFormatException e) {
            throw new LevelLoadException(context + ": expected a number but found '" + s + "'");
        }
    }

    /** Splits on top-level commas only. */
    private static Array<String> splitArgs(String s) {
        Array<String> out = new Array<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                out.add(s.substring(start, i).trim());
                start = i + 1;
            }
        }
        String last = s.substring(start).trim();
        if (!last.isEmpty()) {
            out.add(last);
        }
        return out;
    }
}
