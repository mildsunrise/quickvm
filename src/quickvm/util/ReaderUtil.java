/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package quickvm.util;

import java.io.IOException;
import java.io.Reader;

/**
 *
 * @author xavier
 */
public class ReaderUtil {
    
    public static String readLine(Reader r) throws IOException {
        String str = ""; // Inefficient as fuck, but who cares
        while (true) {
            int c = r.read();
            if (c == -1 && str.isEmpty()) return null;
            if (c == -1 || c == '\n') break;
            str += (char) c;
        }
        return str;
    }
    
}
