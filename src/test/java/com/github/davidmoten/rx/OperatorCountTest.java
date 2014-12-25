package com.github.davidmoten.rx;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;

import com.github.davidmoten.rx.testing.TestingHelper;

public class OperatorCountTest extends TestCase {

    private static final Func1<Observable<String>, Observable<Integer>> COUNT = new Func1<Observable<String>, Observable<Integer>>() {
        @Override
        public Observable<Integer> call(Observable<String> o) {
            return o.count();
        }
    };

    public static TestSuite suite() {

        return TestingHelper.function(COUNT)
        // test empty
                .name("testEmpty").fromEmpty().expect(0)
                // test non-empty count
                .name("testTwo").from("a", "b").expect(2)
                // test single input
                .name("testOne").from("a").expect(1)
                // unsub before completion
                .name("testTwoUnsubscribeAfterOne").from("a", "b", "c").expect(3)
                // get test suites
                .testSuite();
    }

}