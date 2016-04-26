package quickvm;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import quickvm.QuickVM.Frame;
import quickvm.QuickVM.OpHandler;
import quickvm.util.StringUtil;
import quickvm.util.ReaderUtil;
import static quickvm.util.StringUtil.stripComment;

/**
 * Implements the virtual machine operations.
 */
public class Ops {
    
    public static void addAll(Map<String, OpHandler> handlers) {
        
        /** ATTRIBUTES **/
        
        handlers.put(".catch", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                //FIXME: we don't have exceptions for now. just assume that if everything went okay
                //there's no way we can hit a catch
            }
        });
        
        handlers.put(".end", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                throw new IllegalArgumentException("Hit end of method, wtf");
            }
        });
        
        handlers.put(".line", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                //FIXME: add info to frame, or something
            }
        });
        
        handlers.put(".prologue", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                //FIXME: add info to frame, or something
            }
        });
        
        /** CONSTANTS **/
        
        handlers.put("const", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 2);
                int dst = parseRegister(vm, args[0].trim());
                long c = parseIntegerLiteral(args[1].trim());
                
                locals[dst] = (int) c;//FIXME: overflowing casts defined in java?
            }
        });
        
        handlers.put("const-string", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 2);
                int dst = parseRegister(vm, args[0].trim());
                String str = parseStringLiteral(args[1].trim());
                
                int obj = vm.allocateObject(new VMObject.StringObject(str));
                locals[dst] = obj;
            }
        });
        
        handlers.put("fill-array-data", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line_) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line_.split("\\s+", 2)[1].split(",", 2);
                int arr = parseRegister(vm, args[0].trim());
                String label = args[1].trim();
                if (!label.startsWith(":")) throw new IllegalArgumentException("Malformed syntax");
                
                // FIXME: we need to obtain a reader to use temporally, so we
                // open a scope to the same method and then pop it off the stack.
                // This is extremely hacky, I know.
                vm.openScope(vm.stack.peek().name, new int[0]);
                try (Reader r = vm.stack.peek().source) {
                    vm.jumpToLabel(label.substring(1));
                    vm.stack.pop();

                    // Read the array data, filling the array as we go
                    line_ = ReaderUtil.readLine(r).trim();
                    String[] line = line_.split("\\s+", 2);
                    if (!line[0].equals(".array-data"))
                        throw new IllegalArgumentException("Malformed syntax, expected array data: " + line_);
                    VMObject.ArrayObject array = (VMObject.ArrayObject) vm.heap.get(locals[arr]);

                    int offset = 0;
                    while (true) {
                        line_ = stripComment(ReaderUtil.readLine(r)).trim();
                        if (line_.split("\\s+")[0].trim().startsWith(".end")) break;
                        if (!line_.endsWith("t"))
                            throw new IllegalArgumentException("Malformed syntax, expected array data: " + line_);
                        array.value[offset++] = (int) parseIntegerLiteral(line_.substring(0, line_.length()-1));
                    }
                }
            }
        });
        
        /** BRANCHING **/
        
        handlers.put("return-void", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                vm.closeScope();
            }
        });
        
        handlers.put("return-object", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 1);
                int val = parseRegister(vm, args[0].trim());
                
                vm.returnValue = locals[val];
                vm.closeScope();
            }
        });
        
        handlers.put("if-gt", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 3);
                int o1 = parseRegister(vm, args[0].trim());
                int o2 = parseRegister(vm, args[1].trim());
                String label = args[2].trim();
                if (!label.startsWith(":")) throw new IllegalArgumentException("Malformed syntax");
                if (locals[o1] > locals[o2]) vm.jumpToLabel(label.substring(1));
            }
        });
        
        handlers.put("if-ge", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 3);
                int o1 = parseRegister(vm, args[0].trim());
                int o2 = parseRegister(vm, args[1].trim());
                String label = args[2].trim();
                if (!label.startsWith(":")) throw new IllegalArgumentException("Malformed syntax");
                if (locals[o1] >= locals[o2]) vm.jumpToLabel(label.substring(1));
            }
        });
        
        handlers.put("goto", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 1);
                String label = args[0];
                if (!label.startsWith(":")) throw new IllegalArgumentException("Malformed syntax");
                vm.jumpToLabel(label.substring(1));
            }
        });
        
        handlers.put("return-void", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                vm.closeScope();
            }
        });
        
        handlers.put("packed-switch", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line_) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line_.split("\\s+", 2)[1].split(",", 2);
                int val = parseRegister(vm, args[0].trim());
                String label = args[1].trim();
                if (!label.startsWith(":")) throw new IllegalArgumentException("Malformed syntax");
                
                // FIXME: we need to obtain a reader to use temporally, so we
                // open a scope to the same method and then pop it off the stack.
                // This is extremely hacky, I know.
                vm.openScope(vm.stack.peek().name, new int[0]);
                try (Reader r = vm.stack.peek().source) {
                    vm.jumpToLabel(label.substring(1));
                    vm.stack.pop();

                    // Read the packed switch definition, jumping where appropiate
                    line_ = ReaderUtil.readLine(r).trim();
                    String[] line = line_.split("\\s+", 2);
                    if (!line[0].equals(".packed-switch"))
                        throw new IllegalArgumentException("Malformed syntax, expected packed switch: " + line_);
                    int offset = (int) parseIntegerLiteral(line[1]); //FIXME: overflowing casts defined in java?
                    int value = locals[val]; //FIXME: is this what we want? int? probably not, should always work with longs

                    if (value < offset) return;
                    while (true) {
                        line_ = ReaderUtil.readLine(r).trim();
                        if (line_.split("\\s+")[0].trim().startsWith(".end")) break;
                        if (!line_.startsWith(":")) throw new IllegalArgumentException("Malformed syntax, expected label: "+line_);
                        if (value == offset) {
                            vm.jumpToLabel(line_.substring(1));
                            return;
                        }
                        offset++;
                    }
                }
            }
        });
        
        /** REGISTER ACCESS **/
        
        handlers.put("move", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 2);
                int dst = parseRegister(vm, args[0].trim());
                int src = parseRegister(vm, args[1].trim());
                
                locals[dst] = locals[src];
            }
        });
        
        handlers.put("move-object", handlers.get("move"));
        
        handlers.put("move-result", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 1);
                int dst = parseRegister(vm, args[0].trim());
                
                locals[dst] = vm.returnValue;
            }
        });
        
        handlers.put("move-result-object", handlers.get("move-result"));
        
        /** METHOD INVOCATION, INSTANCE CREATION **/
        
        handlers.put("new-instance", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 2);
                int dst = parseRegister(vm, args[0].trim());
                String type = args[1].trim();
                
                if (vm.methods.containsKey(type)) {
                    vm.methods.get(type).handle(vm, new int[0]);
                    locals[dst] = vm.returnValue;
                } else {
                    int obj = vm.allocateObject(new VMObject.VirtualObject(vm, type));
                    locals[dst] = obj;
                }
            }
        });
        
        handlers.put("new-array", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 3);
                int dst = parseRegister(vm, args[0].trim());
                int size = parseRegister(vm, args[1].trim());
                String type = args[2].trim();
                
                locals[dst] = vm.allocateObject(new VMObject.ArrayObject(vm, type, new int[locals[size]]));
            }
        });
        
        handlers.put("invoke-direct", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String args = line.split("\\s+", 2)[1];
                if (!args.startsWith("{") || !args.contains("}")) throw new IllegalArgumentException("Malformed syntax: "+line);
                String params = args.substring(1, args.indexOf("}"));
                String name = args.substring(params.length()+2).split(",", 2)[1].trim();
                
                int[] regs;
                if (params.trim().isEmpty()) {
                    regs = new int[0];
                } else {
                    String[] regsStr = params.split(",");
                    regs = new int[regsStr.length];
                    for (int i = 0; i < regs.length; i++)
                        regs[i] = locals[parseRegister(vm, regsStr[i].trim())];
                }
                
                vm.openScope(name, regs);
            }
        });
        
        handlers.put("invoke-static", handlers.get("invoke-direct"));
        
        //TODO: SOLVE THIS!!
        handlers.put("invoke-virtual", handlers.get("invoke-direct"));
        
        handlers.put("filled-new-array", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String args = line.split("\\s+", 2)[1];
                if (!args.startsWith("{") || !args.contains("}")) throw new IllegalArgumentException("Malformed syntax: "+line);
                String params = args.substring(1, args.indexOf("}"));
                String type = args.substring(params.length()+2).split(",", 2)[1].trim();
                
                int[] regs;
                if (params.trim().isEmpty()) {
                    regs = new int[0];
                } else {
                    String[] regsStr = params.split(",");
                    regs = new int[regsStr.length];
                    for (int i = 0; i < regs.length; i++)
                        regs[i] = locals[parseRegister(vm, regsStr[i].trim())];
                }
                
                vm.returnValue = vm.allocateObject(new VMObject.ArrayObject(vm, type, regs));
            }
        });
        
        /** FIELD & ARRAY MANIPULATION **/
        
        handlers.put("iget", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 3);
                int val = parseRegister(vm, args[0].trim());
                int obj = parseRegister(vm, args[1].trim());
                String field = args[2].trim();
                
                VMObject.VirtualObject vObj = (VMObject.VirtualObject) vm.heap.get(locals[obj]);
                if (vObj.fields.containsKey(field))
                    locals[val] = vObj.fields.get(field);
                else
                    locals[val] = 0;
            }
        });
        
        handlers.put("iget-object", handlers.get("iget"));
        handlers.put("iget-boolean", handlers.get("iget"));
        handlers.put("iget-byte", handlers.get("iget"));
        handlers.put("iget-char", handlers.get("iget"));
        handlers.put("iget-short", handlers.get("iget"));
        
        handlers.put("iput", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 3);
                int val = parseRegister(vm, args[0].trim());
                int obj = parseRegister(vm, args[1].trim());
                String field = args[2].trim();
                String type = field.split(":", 2)[1];
                
                VMObject.VirtualObject vObj = (VMObject.VirtualObject) vm.heap.get(locals[obj]);
                vObj.fields.put(field, locals[val]);
            }
        });
        
        handlers.put("iput-object", handlers.get("iput"));
        handlers.put("iput-boolean", handlers.get("iput"));
        handlers.put("iput-byte", handlers.get("iput"));
        handlers.put("iput-char", handlers.get("iput"));
        handlers.put("iput-short", handlers.get("iput"));
        
        handlers.put("sget", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 2);
                int val = parseRegister(vm, args[0].trim());
                String field = args[1].trim();
                
                if (vm.staticFields.containsKey(field))
                    locals[val] = vm.staticFields.get(field);
                else
                    locals[val] = 0;
            }
        });
        
        handlers.put("sget-object", handlers.get("sget"));
        handlers.put("sget-boolean", handlers.get("sget"));
        handlers.put("sget-byte", handlers.get("sget"));
        handlers.put("sget-char", handlers.get("sget"));
        handlers.put("sget-short", handlers.get("sget"));
        
        handlers.put("sput", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 2);
                int val = parseRegister(vm, args[0].trim());
                String field = args[1].trim();
                String type = field.split(":", 2)[1];
                
                if (type.charAt(0) == 'L' || type.charAt(0) == '[') {
                    VMObject obj = vm.heap.get(locals[val]);
                    System.out.printf("%s = %s\n", field, obj);
                    if (obj != null && !obj.getType().equals(type))
                        System.err.println("Warning! Types don't match: " + obj + " for " + field);
                } else {
                    System.out.printf("%s = %s\n", field, locals[val]);
                }
                vm.staticFields.put(field, locals[val]);
            }
        });
        
        handlers.put("sput-object", handlers.get("sput"));
        handlers.put("sput-boolean", handlers.get("sput"));
        handlers.put("sput-byte", handlers.get("sput"));
        handlers.put("sput-char", handlers.get("sput"));
        handlers.put("sput-short", handlers.get("sput"));
        
        handlers.put("aget", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 3);
                int val = parseRegister(vm, args[0].trim());
                int arr = parseRegister(vm, args[1].trim());
                int idx = parseRegister(vm, args[2].trim());
                
                VMObject.ArrayObject arrObj = (VMObject.ArrayObject) vm.heap.get(locals[arr]);
                locals[val] = arrObj.value[locals[idx]];
            }
        });
        
        handlers.put("aget-object", handlers.get("aget"));
        handlers.put("aget-boolean", handlers.get("aget"));
        handlers.put("aget-byte", handlers.get("aget"));
        handlers.put("aget-char", handlers.get("aget"));
        handlers.put("aget-short", handlers.get("aget"));
        
        handlers.put("aput", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 3);
                int val = parseRegister(vm, args[0].trim());
                int arr = parseRegister(vm, args[1].trim());
                int idx = parseRegister(vm, args[2].trim());
                
                VMObject.ArrayObject arrObj = (VMObject.ArrayObject) vm.heap.get(locals[arr]);
                arrObj.value[locals[idx]] = locals[val];
            }
        });
        
        handlers.put("aput-object", handlers.get("aput"));
        handlers.put("aput-boolean", handlers.get("aput"));
        handlers.put("aput-byte", handlers.get("aput"));
        handlers.put("aput-char", handlers.get("aput"));
        handlers.put("aput-short", handlers.get("aput"));
        
        handlers.put("array-length", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 2);
                int dst = parseRegister(vm, args[0].trim());
                int arr = parseRegister(vm, args[1].trim());
                
                VMObject.ArrayObject arrObj = (VMObject.ArrayObject) vm.heap.get(locals[arr]);
                locals[dst] = arrObj.value.length;
            }
        });
        
        /** ARITHMETICS, BITWISE OPS, LENGTH MOD **/
        
        handlers.put("add-int", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 3);
                int dst = parseRegister(vm, args[0].trim());
                int o1 = readValue(vm, args[args.length - 2].trim());
                int o2 = readValue(vm, args[args.length - 1].trim());
                
                locals[dst] = o1 + o2;
            }
        });
        
        handlers.put("rem-int", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 3);
                int dst = parseRegister(vm, args[0].trim());
                int o1 = readValue(vm, args[args.length - 2].trim());
                int o2 = readValue(vm, args[args.length - 1].trim());
                
                locals[dst] = o1 % o2;
            }
        });
        
        handlers.put("xor-int", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 3);
                int dst = parseRegister(vm, args[0].trim());
                int o1 = readValue(vm, args[args.length - 2].trim());
                int o2 = readValue(vm, args[args.length - 1].trim());
                
                locals[dst] = o1 ^ o2;
            }
        });
        
        handlers.put("int-to-char", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                int[] locals = vm.stack.peek().locals;
                String[] args = line.split("\\s+", 2)[1].split(",", 2);
                int dst = parseRegister(vm, args[0].trim());
                int src = parseRegister(vm, args[1].trim());
                
                //FIXME: are overflowing casts defined in java?
                locals[dst] = (char) locals[src];
            }
        });
        
        /** MISCELLANEOUS **/
        
        /*handlers.put("check-cast", new OpHandler() {
            @Override
            public void handle(QuickVM vm, String line) throws IOException {
                //FIXME: implement this when we support exceptions
            }
        });*/
        
    }
    
    /* Parsing utilities */
    
    public static int parseRegister(QuickVM vm, String reg) {
        if (reg.startsWith("v")) {
            return Integer.parseInt(reg.substring(1));
        }
        if (reg.startsWith("p")) {
            Frame f = vm.stack.peek();
            return Integer.parseInt(reg.substring(1)) + (f.locals.length - f.parameters);
        }
        throw new IllegalArgumentException("Invalid register string: "+reg);
    }
    
    public static String parseStringLiteral(String str) {
        if (!str.startsWith("\"") || !str.endsWith("\""))
            throw new IllegalArgumentException("Invalid string literal");
        return StringUtil.unescape_perl_string(str.substring(1, str.length()-1));
    }
    
    public static long parseIntegerLiteral(String str) {
        long multiplier = 1;
        int radix = 10;
        
        if (str.startsWith("-") || str.startsWith("+")) {
            if (str.startsWith("-")) multiplier = -1;
            str = str.substring(1);
        }
        
        if (str.startsWith("0x")) {
            str = str.substring(2);
            radix = 16;
        }
        return multiplier * Long.parseLong(str, radix);
    }
    
    public static int readValue(QuickVM vm, String str) {
        try {
            return vm.stack.peek().locals[parseRegister(vm, str)];
        } catch (IllegalArgumentException ex) {
            return (int) parseIntegerLiteral(str);
        }
    }
    
}
