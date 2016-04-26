package quickvm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import quickvm.util.StringUtil;

/**
 * VM object, allocated in the "heap" and pointable.
 */
public interface VMObject {
    
    public String getType();
    
    /**
     * "Real" VM object, aka with field registers.
     */
    public static class VirtualObject implements VMObject {
        public final QuickVM vm;
        public final String type;
        Map<String, Integer> fields = new HashMap<>();

        public VirtualObject(QuickVM vm, String type) {
            this.vm = vm;
            this.type = type;
        }

        @Override
        public String toString() {
            return type + "{" + "fields=" + fields + '}';
        }

        @Override
        public String getType() {
            return type;
        }
    }

    /**
     * String objects are allocated natively for speed and simplicity.
     */
    public class StringObject implements VMObject {
        public String value;

        public StringObject(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return StringUtil.escapeString(value);
        }

        @Override
        public String getType() {
            return "Ljava/lang/String;";
        }
    }

    /**
     * StringBuilder objects are allocated natively for speed and simplicity.
     */
    public class StringBuilderObject implements VMObject {
        public StringBuilder value = new StringBuilder();

        @Override
        public String toString() {
            return "StringBuilder{ " + StringUtil.escapeString(value.toString()) + " }";
        }

        @Override
        public String getType() {
            return "Ljava/lang/StringBuilder;";
        }
    }

    /**
     * ArrayList objects are allocated natively for speed and simplicity.
     */
    public class ArrayListObject implements VMObject {
        public final QuickVM vm;
        public ArrayList<Integer> value;

        public ArrayListObject(QuickVM vm) {
            this.vm = vm;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ArrayList[ ");
            for (int i = 0; i < value.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(vm.heap.get(value.get(i)));
            }
            sb.append(" ]");
            return sb.toString();
        }

        @Override
        public String getType() {
            return "Ljava/util/ArrayList;";
        }
    }

    /**
     * HashMap objects are allocated natively for speed and simplicity.
     * TODO: use hashcode
     */
    public class HashMapObject implements VMObject {
        public final QuickVM vm;
        public HashMap<Integer, Integer> value;

        public HashMapObject(QuickVM vm) {
            this.vm = vm;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("HashMap{ ");
            boolean emitted = false;
            for (Integer key : value.keySet()) {
                if (emitted) sb.append(", ");
                emitted = true;
                sb.append(vm.heap.get(key));
                sb.append("=");
                sb.append(vm.heap.get(value.get(key)));
            }
            sb.append(" }");
            return sb.toString();
        }

        @Override
        public String getType() {
            return "Ljava/util/HashMap;";
        }
    }

    /**
     * HashSet objects are allocated natively for speed and simplicity.
     * TODO: use hashcode
     */
    public class HashSetObject implements VMObject {
        public final QuickVM vm;
        public HashSet<Integer> value;

        public HashSetObject(QuickVM vm) {
            this.vm = vm;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("HashSet{ ");
            boolean emitted = false;
            for (Integer key : value) {
                if (emitted) sb.append(", ");
                emitted = true;
                sb.append(vm.heap.get(key));
            }
            sb.append(" }");
            return sb.toString();
        }

        @Override
        public String getType() {
            return "Ljava/util/HashSet;";
        }
    }
    
    /**
     * Array objects are allocated natively, of course.
     */
    public class ArrayObject implements VMObject {
        public final QuickVM vm;
        public final String type;
        public final int[] value;
        
        public ArrayObject(QuickVM vm, String type, int[] value) {
            this.vm = vm;
            this.type = type;
            this.value = value;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[ ");
            for (int i = 0; i < value.length; i++) {
                if (i > 0) sb.append(", ");
                int val = value[i];
                if (type.charAt(1) == 'L' || type.charAt(1) == '[') {
                    sb.append(vm.heap.get(val));
                } else {
                    sb.append(val);
                }
            }
            sb.append(" ]");
            return sb.toString();
        }
        
        @Override
        public String getType() {
            return type;
        }
        
    }
    
}
