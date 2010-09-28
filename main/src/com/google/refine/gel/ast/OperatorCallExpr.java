package com.google.refine.gel.ast;

import java.util.Properties;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.ExpressionUtils;

/**
 * An abstract syntax tree node encapsulating an operator call, such as "+".
 */
public class OperatorCallExpr implements Evaluable {
    final protected Evaluable[] _args;
    final protected String        _op;
    
    public OperatorCallExpr(Evaluable[] args, String op) {
        _args = args;
        _op = op;
    }
                              
    public Object evaluate(Properties bindings) {
        Object[] args = new Object[_args.length];
        for (int i = 0; i < _args.length; i++) {
            Object v = _args[i].evaluate(bindings);
            if (ExpressionUtils.isError(v)) {
                return v;
            }
            args[i] = v;
        }
        
        if (args.length == 2) {
            if (args[0] != null && args[1] != null) {
                if (isIntegral(args[0]) && isIntegral(args[1])) {
                    long n1 = ((Number) args[0]).longValue();
                    long n2 = ((Number) args[1]).longValue();
                    
                    if ("+".equals(_op)) {
                        return n1 + n2;
                    } else if ("-".equals(_op)) {
                        return n1 - n2;
                    } else if ("*".equals(_op)) {
                        return n1 * n2;
                    } else if ("/".equals(_op)) {
                        return n1 / n2;
                    } else if (">".equals(_op)) {
                        return n1 > n2;
                    } else if (">=".equals(_op)) {
                        return n1 >= n2;
                    } else if ("<".equals(_op)) {
                        return n1 < n2;
                    } else if ("<=".equals(_op)) {
                        return n1 <= n2;
                    }
                } else if (args[0] instanceof Number && args[1] instanceof Number) {
                    double n1 = ((Number) args[0]).doubleValue();
                    double n2 = ((Number) args[1]).doubleValue();
                    
                    if ("+".equals(_op)) {
                        return n1 + n2;
                    } else if ("-".equals(_op)) {
                        return n1 - n2;
                    } else if ("*".equals(_op)) {
                        return n1 * n2;
                    } else if ("/".equals(_op)) {
                        return n1 / n2;
                    } else if (">".equals(_op)) {
                        return n1 > n2;
                    } else if (">=".equals(_op)) {
                        return n1 >= n2;
                    } else if ("<".equals(_op)) {
                        return n1 < n2;
                    } else if ("<=".equals(_op)) {
                        return n1 <= n2;
                    }
                }
                
                if ("+".equals(_op)) {
                    return args[0].toString() + args[1].toString();
                }
            }
            
            if ("==".equals(_op)) {
                if (args[0] != null) {
                    return args[0].equals(args[1]);
                } else {
                    return args[1] == null;
                }
            } else if ("!=".equals(_op)) {
                if (args[0] != null) {
                    return !args[0].equals(args[1]);
                } else {
                    return args[1] != null;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        
        for (Evaluable ev : _args) {
            if (sb.length() > 0) {
                sb.append(' ');
                sb.append(_op);
                sb.append(' ');
            }
            sb.append(ev.toString());
        }
        
        return sb.toString();
    }
    
    private boolean isIntegral(Object n) {
        return n instanceof Long || n instanceof Integer;
    }
}
