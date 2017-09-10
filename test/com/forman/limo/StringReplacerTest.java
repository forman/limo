package com.forman.limo;

import org.junit.Test;

import java.util.HashMap;

import static junit.framework.TestCase.assertEquals;

public class StringReplacerTest {
    @Test
    public void testIt() throws Exception {
        HashMap<String, String> R = new HashMap<>();
        assertEquals("IMAG8", StringReplacer.replace("IMAG{N}", 8, R));
        assertEquals("IMAG317", StringReplacer.replace("IMAG{N}", 317, R));

        assertEquals("IMAG08", StringReplacer.replace("IMAG{0N}", 8, R));
        assertEquals("IMAG317", StringReplacer.replace("IMAG{0N}", 317, R));

        assertEquals("IMAG008", StringReplacer.replace("IMAG{00N}", 8, R));
        assertEquals("IMAG317", StringReplacer.replace("IMAG{00N}", 317, R));

        assertEquals("IMAG00008", StringReplacer.replace("IMAG{0000N}", 8, R));
        assertEquals("IMAG00317", StringReplacer.replace("IMAG{0000N}", 317, R));

        assertEquals("PIC0000327_P8080034_DxO", StringReplacer.replace("PIC{000000N}_{NAME}", 327, new HashMap<String, String>(){{put("{NAME}", "P8080034_DxO");}}));
    }
}
