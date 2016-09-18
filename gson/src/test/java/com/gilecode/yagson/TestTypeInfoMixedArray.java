package com.gilecode.yagson;

import junit.framework.TestCase;

import java.math.BigDecimal;

import static com.gilecode.yagson.TestingUtils.jsonStr;

public class TestTypeInfoMixedArray extends TestCase {

    private ClassWithMixedArray objToTestWithNumberArray() {
        return new ClassWithMixedArray<Number>(null, 1, 2L, 3.01f, 4.02, BigDecimal.ONE);
    }

    private ClassWithMixedArray objToTestWithObjectArray() {
        return new ClassWithMixedArray<Object>(null, 1, 2L, 3.01f, 4.02, BigDecimal.ONE);
    }

    public void testMixedNumbersInNumberArray() {
        ClassWithMixedArray obj = objToTestWithNumberArray();
        TestingUtils.testFully(obj, jsonStr("{'arr':" +
                "{'@type':'[Ljava.lang.Number;'," +
                "'@val':[null," +
                "{'@type':'java.lang.Integer','@val':1}," +
                "2," +
                "{'@type':'java.lang.Float','@val':3.01}," +
                "4.02," +
                "{'@type':'java.math.BigDecimal','@val':1}]}}"));
    }

    public void testMixedNumbersInObjectArray() {
        ClassWithMixedArray obj = objToTestWithObjectArray();
        TestingUtils.testFully(obj, jsonStr("{'arr':[null," +
                "{'@type':'java.lang.Integer','@val':1}," +
                "2," +
                "{'@type':'java.lang.Float','@val':3.01}," +
                "4.02," +
                "{'@type':'java.math.BigDecimal','@val':1}]}"));
    }

    public void testCustomInMixedObjectArray() {
        ClassWithMixedArray obj = new ClassWithMixedArray<Object>(new Person("foo", "bar"));

        TestingUtils.testFullyByToString(obj, jsonStr(
                "{'arr':[" +
                "{'@type':'com.gilecode.yagson.Person'," +
                "'@val':{'name':'foo','family':'bar'}}]}"));
    }

    public void testCustomInMixedCustomArray() {
        ClassWithMixedArray obj = new ClassWithMixedArray<Person>(new Person("foo", "bar"));

        TestingUtils.testFullyByToString(obj, jsonStr("{'arr':" +
                "{'@type':'[Lcom.gilecode.yagson.Person;'," +
                "'@val':[{'name':'foo','family':'bar'}]}}"));
    }

    public void testPureCustomArray() {
        ClassWithPersonArray obj = new ClassWithPersonArray(new Person("foo", "bar"));

        TestingUtils.testFullyByToString(obj, jsonStr(
                "{'arr':[{'name':'foo','family':'bar'}]}"));
    }
}
