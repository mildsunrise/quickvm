package quickvm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static quickvm.util.ReaderUtil.readLine;
import static quickvm.util.StringUtil.stripComment;

/**
 * Barebones Smali VM. Probably the dirtiest code I've ever written.
 * DOESN'T EVEN HAVE GC. NOT EVEN A GC. And it assumes the bytecode given
 * is well-formed and verifies on a real VM.
 * 
 * Does not support exceptions, virtual methods nor 64-bit operations at the moment.
 */
public class QuickVM {
    
    public static interface OpHandler {
        void handle(QuickVM vm, String line) throws IOException;
    }
    
    public static interface MethodHandler {
        void handle(QuickVM vm, int[] params);
    }
    
    public static class Frame {
        /**
         * Reader, shifted right at the start of the new instruction to execute
         * at this scope. Reader should be marked after start of scope.
         */
        Reader source;
        
        /** Local registers. */
        int[] locals;
        
        /** Parameters scope was invoked with. */
        int parameters;
        
        /** Scope name (method name). */
        String name;
    }
    
    /** Call stack. */
    Deque<Frame> stack = new ArrayDeque<>();
    
    /** Root directory where smali is stored. */
    File root;
    
    /** Return register. */
    int returnValue;
    
    /** Op handlers. */
    Map<String, OpHandler> handlers = new HashMap<>();
    
    /** Native methods. */
    Map<String, MethodHandler> methods = new HashMap<>();
    
    /** Static fields. */
    Map<String, Integer> staticFields = new HashMap<>();
    
    /** Object "heap". */
    List<VMObject> heap = new ArrayList<>();
    
    /** Initialized classes. */
    Set<String> initializedClasses = new HashSet<>();
    
    /** Last executed (not necessarily completed) command. */
    String lastCommand;
    
    public void jumpToLabel(String label) throws IOException {
        Reader source = stack.peek().source;
        source.reset();
        while (true) {
            String line = readLine(source).trim();
            if (line.equals(":"+label)) break;
        }
    }
    
    public boolean initializeType(String className) throws IOException {
        if (initializedClasses.contains(className)) return false;
        try {
            openScope("L" + className + ";-><clinit>()V", new int[0]);
            return true;
        } catch (IllegalArgumentException ex) {
            if (!ex.getMessage().equals("Method not found")) throw ex;
        }
        return false;
    }
    
    public void openScope(String name, int[] params) throws IOException {
        if (methods.containsKey(name)) {
            methods.get(name).handle(this, params);
            return;
        }
        if (name.charAt(0) == '[') {
            methods.get("[->" + name.split("->", 2)[1]).handle(this, params);
            return;
        }
        if (name.charAt(0) != 'L')
            throw new IllegalArgumentException("What the fuck");
        
        if (stack.size() > 32)
            throw new IllegalStateException("Too much fucking recursion");
        
        String className = name.split(";")[0].substring(1);
        String methodName = name.split("->")[1];
        File file = new File(root, className + ".smali");
        
        Reader source = null;
        //try {
            try {
                source = new BufferedReader(new FileReader(file));
            } catch (FileNotFoundException ex) {
                throw new IllegalArgumentException("Class not found for: " + name, ex);
            }
            
            
            while (true) {
                String line_ = readLine(source);
                if (line_ == null) throw new IllegalArgumentException("Method not found");
                String[] line = line_.trim().split("\\s+");
                if (line[0].equals(".method") && line[line.length-1].equals(methodName))
                    break;
            }

            int regs;
            String[] line = readLine(source).trim().split("\\s+", 2);
            if (line[0].equals(".locals")) {
                regs = params.length + Integer.parseInt(line[1]);
            } else if (line[0].equals(".registers")) {
                regs = Integer.parseInt(line[1]);
            } else {
                throw new IllegalArgumentException("Expected .locals or .registers");
            }
            source.mark(0x10000);
        
            Frame f = new Frame();
            f.name = name;
            f.locals = new int[regs];
            f.parameters = params.length;
            System.arraycopy(params, 0, f.locals, f.locals.length - params.length, params.length);
            f.source = source;
            stack.push(f);
            source = null;
        /*} finally {
            if (source != null) source.close();
        }*/
        
        if (methodName.equals("<clinit>()V"))
            initializedClasses.add(className);
        else
            initializeType(className);
    }
    
    public void closeScope() throws IOException {
        stack.pop().source.close();
    }
    
    public void step() throws IOException {
        Reader source = stack.peek().source;
        while (true) {
            int c = source.read();
            if (c == -1) throw new IllegalArgumentException("Unexpected end of file");
            if (Character.isWhitespace(c) || c == '#') continue;
            
            // Skip labels
            if (c == ':') {
                readLine(source);
                continue;
            }
            
            // Process everything else
            String line = ((char)c) + readLine(source);
            lastCommand = line;
            line = stripComment(line).trim();
            String name = line.split("\\s+")[0].split("/", 2)[0].trim();
            if (!handlers.containsKey(name))
                throw new IllegalArgumentException("Unknown command:\n" + line);
            handlers.get(name).handle(this, line);
            break;
        }
    }
    
    public int allocateObject(VMObject obj) {
        int idx = heap.size();
        heap.add(obj);
        return idx;
    }
    
    public void reset() throws IOException {
        while (!stack.isEmpty()) closeScope();
        staticFields.clear();
        heap.clear();
        heap.add(null);
        initializedClasses.clear();
        returnValue = 0;
    }
    
    public void dumpState() {
        System.err.println(" Last command:\n    " + lastCommand);
        for (Frame f : stack) {
            System.err.printf("\n * %s\n   Registers:", f.name);
            int regslength = 0;
            for (int i = 0; i < f.locals.length; i++) {
                if (f.locals[i] != 0) regslength = i+1;
            }
            for (int i = 0; i < regslength; i++) {
                if (i % 5 == 0) System.err.printf("\n   ");
                System.err.printf("  %08x", f.locals[i]);
            }
            System.err.printf("\n   Source:\n");
            try {
                System.err.println("     | " + readLine(f.source));
                System.err.println("     | " + readLine(f.source));
                System.err.println("     | " + readLine(f.source));
            } catch (IOException ex1) {
                Logger.getLogger(QuickVM.class.getName()).log(Level.SEVERE, null, ex1);
            }
            System.err.println("     ...");
        }
    }
    
    
    
    public final static boolean VERBOSE = true;
    
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        
        QuickVM vm = new QuickVM();
        vm.root = new File(args[0]);
        Ops.addAll(vm.handlers);
        Methods.addAll(vm.methods);
        vm.heap.add(null);
        
        int total = 0, failed = 0;
        while (true) {
            String file;
            try {
                file = sc.nextLine();
            } catch (NoSuchElementException ex) {
                break;
            }
            System.err.println("Trying file: " + file);
            total++;

            try {
                if (!file.endsWith(".smali")) throw new IllegalArgumentException("Expected smali file path");
                vm.initializeType(file.substring(0, file.length() - 6));
                while (vm.stack.size() > 0) vm.step();
            } catch (Exception ex) {
                if (VERBOSE) {
                    System.err.println();
                    Logger.getLogger(QuickVM.class.getName()).log(Level.SEVERE, null, ex);
                    System.err.println("VM state:");
                    vm.dumpState();
                }
                System.err.println("File failed: " + file);
                failed++;
            }
            try {
                vm.reset();
            } catch (IOException ex) {
                Logger.getLogger(QuickVM.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        System.err.printf("Finished: %d classes from %d failed to load (%.2f%%).\n", failed, total, (failed / (float) total) * 100);
        System.err.println("Sanity tests you could perform:\n - Check for type cast warnings on stderr.\n - Check for \\u00 escapes on the constants.\n - Manually verify failed classes.\n - Check for string constants outside static constructors.");
    }
    
}
