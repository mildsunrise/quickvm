/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package quickvm;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 *
 * @author xavier
 */
public class Methods {
    public static void addAll(Map<String, QuickVM.MethodHandler> handlers) {
        
        handlers.put("Ljava/lang/Object;-><init>()V", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
            }
        });
        
        handlers.put("[->clone()Ljava/lang/Object;", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.ArrayObject arr = (VMObject.ArrayObject) vm.heap.get(params[0]);
                vm.returnValue = vm.allocateObject(new VMObject.ArrayObject(vm, arr.type, arr.value.clone()));
            }
        });
        
        /** java.lang.String **/
        
        handlers.put("Ljava/lang/String;", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                vm.returnValue = vm.allocateObject(new VMObject.StringObject(null));
            }
        });
        
        handlers.put("Ljava/lang/String;-><init>([C)V", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                int[] locals = vm.stack.peek().locals;
                VMObject.StringObject str = (VMObject.StringObject) vm.heap.get(params[0]);
                VMObject.ArrayObject arr = (VMObject.ArrayObject) vm.heap.get(params[1]);
                char[] characters = new char[arr.value.length];
                for (int i = 0; i < characters.length; i++)
                    characters[i] = (char) arr.value[i];
                str.value = new String(characters);
            }
        });
        
        handlers.put("Ljava/lang/String;->intern()Ljava/lang/String;", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                // FIXME: yeah, right... we don't have a GC and you're asking me to recycle references...
                vm.returnValue = params[0];
            }
        });
        
        handlers.put("Ljava/lang/String;->charAt(I)C", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.StringObject str = (VMObject.StringObject) vm.heap.get(params[0]);
                vm.returnValue = str.value.charAt(params[1]);
            }
        });
        
        handlers.put("Ljava/lang/String;->length()I", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.StringObject str = (VMObject.StringObject) vm.heap.get(params[0]);
                vm.returnValue = str.value.length();
            }
        });
        
        handlers.put("Ljava/lang/String;->toCharArray()[C", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                int[] locals = vm.stack.peek().locals;
                VMObject.StringObject str = (VMObject.StringObject) vm.heap.get(params[0]);
                int[] characters = new int[str.value.length()];
                for (int i = 0; i < characters.length; i++)
                    characters[i] = str.value.charAt(i); //FIXME: probably not what we want if char is negative, we shouldn't extend sign
                vm.returnValue = vm.allocateObject(new VMObject.ArrayObject(vm, "[C", characters));
            }
        });
        
        handlers.put("Ljava/lang/String;->getBytes()[B", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                int[] locals = vm.stack.peek().locals;
                VMObject.StringObject str = (VMObject.StringObject) vm.heap.get(params[0]);
                byte[] bytes = str.value.getBytes();
                int[] result = new int[bytes.length];
                for (int i = 0; i < result.length; i++)
                    result[i] = bytes[i]; //FIXME: probably not what we want if char is negative, we shouldn't extend sign
                vm.returnValue = vm.allocateObject(new VMObject.ArrayObject(vm, "[B", result));
            }
        });
        
        handlers.put("Ljava/lang/String;->getBytes(Ljava/lang/String;)[B", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                int[] locals = vm.stack.peek().locals;
                VMObject.StringObject str = (VMObject.StringObject) vm.heap.get(params[0]);
                VMObject.StringObject arg = (VMObject.StringObject) vm.heap.get(params[1]);
                byte[] bytes;
                try {
                    bytes = str.value.getBytes(arg.value);
                } catch (UnsupportedEncodingException ex) {
                    throw new IllegalStateException("Unsupported enconding requested: " + arg.value);
                }
                int[] result = new int[bytes.length];
                for (int i = 0; i < result.length; i++)
                    result[i] = bytes[i]; //FIXME: probably not what we want if char is negative, we shouldn't extend sign
                vm.returnValue = vm.allocateObject(new VMObject.ArrayObject(vm, "[B", result));
            }
        });
        
        /** java.lang.StringBuilder **/
        
        handlers.put("Ljava/lang/StringBuilder;", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                vm.returnValue = vm.allocateObject(new VMObject.StringBuilderObject());
            }
        });
        
        handlers.put("Ljava/lang/StringBuilder;-><init>()V", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
            }
        });
        
        handlers.put("Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.StringBuilderObject sbObj = (VMObject.StringBuilderObject) vm.heap.get(params[0]);
                VMObject.StringObject str = (VMObject.StringObject) vm.heap.get(params[1]);
                sbObj.value.append(str.value);
            }
        });
        
        handlers.put("Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.StringBuilderObject sbObj = (VMObject.StringBuilderObject) vm.heap.get(params[0]);
                sbObj.value.append((char) params[1]);
                vm.returnValue = params[0];
            }
        });
        
        handlers.put("Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.StringBuilderObject sbObj = (VMObject.StringBuilderObject) vm.heap.get(params[0]);
                VMObject.StringObject str = (VMObject.StringObject) vm.heap.get(params[1]);
                sbObj.value.append(str.value);
                vm.returnValue = params[0];
            }
        });
        
        handlers.put("Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.StringBuilderObject sbObj = (VMObject.StringBuilderObject) vm.heap.get(params[0]);
                sbObj.value.append(params[1]);
                vm.returnValue = params[0];
            }
        });
        
        handlers.put("Ljava/lang/StringBuilder;->toString()Ljava/lang/String;", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.StringBuilderObject sbObj = (VMObject.StringBuilderObject) vm.heap.get(params[0]);
                vm.returnValue = vm.allocateObject(new VMObject.StringObject(sbObj.value.toString()));
            }
        });
        
        /** java.lang.Enum **/
        
        handlers.put("Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                //FIXME: not interesting at the moment
            }
        });
        
        /** java.util.ArrayList **/
        
        handlers.put("Ljava/util/ArrayList;", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                vm.returnValue = vm.allocateObject(new VMObject.ArrayListObject(vm));
            }
        });
        
        handlers.put("Ljava/util/ArrayList;-><init>()V", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.ArrayListObject alObj = (VMObject.ArrayListObject) vm.heap.get(params[0]);
                alObj.value = new ArrayList<>();
            }
        });
        
        handlers.put("Ljava/util/ArrayList;->add(Ljava/lang/Object;)V", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.ArrayListObject alObj = (VMObject.ArrayListObject) vm.heap.get(params[0]);
                alObj.value.add(params[1]);
            }
        });
        
        handlers.put("Ljava/util/ArrayList;->get(I)Ljava/lang/Object;", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.ArrayListObject alObj = (VMObject.ArrayListObject) vm.heap.get(params[0]);
                vm.returnValue = alObj.value.get(params[1]);
            }
        });
        
        handlers.put("Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.ArrayObject arr = (VMObject.ArrayObject) vm.heap.get(params[0]);
                VMObject.ArrayListObject alObj = new VMObject.ArrayListObject(vm);
                alObj.value = new ArrayList<>(arr.value.length);
                for (int o : arr.value) alObj.value.add(o);
                vm.returnValue = vm.allocateObject(alObj);
            }
        });
        
        handlers.put("Ljava/util/Collections;->emptyList()Ljava/util/List;", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.ArrayListObject obj = new VMObject.ArrayListObject(vm);
                obj.value = new ArrayList<>();
                vm.returnValue = vm.allocateObject(obj);
            }
        });
        
        /** java.util.HashMap **/
        
        handlers.put("Ljava/util/HashMap;", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                vm.returnValue = vm.allocateObject(new VMObject.HashMapObject(vm));
            }
        });
        
        handlers.put("Ljava/util/HashMap;-><init>()V", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.HashMapObject hObj = (VMObject.HashMapObject) vm.heap.get(params[0]);
                hObj.value = new HashMap<>();
            }
        });
        
        /*handlers.put("Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.HashMapObject hObj = (VMObject.HashMapObject) vm.heap.get(params[0]);
                vm.returnValue = hObj.value.get(params[1]);
            }
        });*/
        
        handlers.put("Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.HashMapObject hObj = (VMObject.HashMapObject) vm.heap.get(params[0]);
                vm.returnValue = hObj.value.put(params[1], params[2]);
            }
        });
        
        handlers.put("Ljava/util/Collections;->emptyMap()Ljava/util/Map;", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.HashMapObject obj = new VMObject.HashMapObject(vm);
                obj.value = new HashMap<>();
                vm.returnValue = vm.allocateObject(obj);
            }
        });
        
        /** java.util.HashSet **/
        
        handlers.put("Ljava/util/HashSet;", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                vm.returnValue = vm.allocateObject(new VMObject.HashSetObject(vm));
            }
        });
        
        handlers.put("Ljava/util/HashSet;-><init>()V", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.HashSetObject hObj = (VMObject.HashSetObject) vm.heap.get(params[0]);
                hObj.value = new HashSet<>();
            }
        });
        
        handlers.put("Ljava/util/HashSet;-><init>(Ljava/util/Collection;)V", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.HashSetObject hObj = (VMObject.HashSetObject) vm.heap.get(params[0]);
                VMObject.ArrayListObject arr = (VMObject.ArrayListObject) vm.heap.get(params[1]);
                hObj.value = new HashSet<>(arr.value);
            }
        });
        
        handlers.put("Ljava/util/HashSet;->add(Ljava/lang/Object;)Z", new QuickVM.MethodHandler() {
            @Override
            public void handle(QuickVM vm, int[] params) {
                VMObject.HashSetObject hObj = (VMObject.HashSetObject) vm.heap.get(params[0]);
                vm.returnValue = hObj.value.add(params[1]) ? 1 : 0;
            }
        });
        
        /** TODO:
LinkedHashMap
Hashtable
*/
        
    }
}
