package de.fhg.ipa.fhg.aas_transformer.service.test;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class JinjaTest {

    @Test
    public void multipleFunctions() {

        Jinjava jinjava = new Jinjava();
        Map<String, Object> context = new HashMap<>();

        jinjava.getGlobalContext().registerFunction(new ELFunctionDefinition("myfn", "fn1",
                JinjaTest.class, "testFun1", Integer.class));
        jinjava.getGlobalContext().registerFunction(new ELFunctionDefinition("myfn", "fn2",
                JinjaTest.class, "testFun2", Integer.class));

        var template = "Test {{ myfn:fn1(myfn:fn2(2)) }}";

        String renderedTemplate = jinjava.render(template, context);
    }

    public static int testFun1(Integer testParam1) {
        return testParam1;
    }

    public static int testFun2(Integer testParam2) {
        return testParam2 * 2;
    }
}
