package com.github.davidmoten.rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.rx.testing.TestSubscriber2;
import com.github.davidmoten.rx.testing.TestingHelper;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func2;

public class OnSubscribeMatchTest {

    @Test
    public void test() {
        Observable<Integer> a = Observable.just(1, 2);
        Observable<Integer> b = Observable.just(2, 1);
        match(a, b, 2, 1);
    }

    @Test
    public void test2() {
        Observable<Integer> a = Observable.just(1, 2);
        Observable<Integer> b = Observable.just(1, 2);
        match(a, b, 1, 2);
    }

    @Test
    public void test3() {
        Observable<Integer> a = Observable.just(1, 2, 3);
        Observable<Integer> b = Observable.just(3, 2, 1);
        match(a, b, 3, 2, 1);
    }

    @Test
    public void testOneMatch() {
        Observable<Integer> a = Observable.just(1);
        Observable<Integer> b = Observable.just(1);
        match(a, b, 1);
    }

    @Test
    public void testEmpties() {
        Observable<Integer> a = Observable.empty();
        Observable<Integer> b = Observable.empty();
        match(a, b);
    }

    @Test
    public void testRepeats() {
        Observable<Integer> a = Observable.just(1, 1);
        Observable<Integer> b = Observable.just(1, 1);
        match(a, b, 1, 1);
    }

    @Test
    public void testRepeats2() {
        Observable<Integer> a = Observable.just(1, 1, 2, 3, 1);
        Observable<Integer> b = Observable.just(1, 2, 1, 3, 1);
        match(a, b, 1, 2, 1, 3, 1);
    }

    @Test
    public void testNoMatchExistsForAtLeastOneFirstLonger() {
        Observable<Integer> a = Observable.just(1, 2);
        Observable<Integer> b = Observable.just(1);
        match(a, b, 1);
    }

    @Test
    public void testNoMatchExistsForAtLeastOneSameLength() {
        Observable<Integer> a = Observable.just(1, 2);
        Observable<Integer> b = Observable.just(1, 3);
        match(a, b, 1);
    }

    @Test
    public void testNoMatchExistsForAtLeastOneSecondLonger() {
        Observable<Integer> a = Observable.just(1);
        Observable<Integer> b = Observable.just(1, 2);
        match(a, b, 1);
    }

    @Test
    public void testNoMatchExistsAtAll() {
        Observable<Integer> a = Observable.just(1, 2);
        Observable<Integer> b = Observable.just(3, 4);
        match(a, b, new Integer[] {});
    }

    @Test
    public void testBackpressure1() {
        Observable<Integer> a = Observable.just(1, 2, 5, 7, 6, 8);
        Observable<Integer> b = Observable.just(3, 4, 5, 6, 7);
        matchThem(a, b) //
                .to(TestingHelper.<Integer> testWithRequest(0)) //
                .assertNoValues()//
                .assertNoTerminalEvent()//
                .requestMore(1)//
                .assertValuesAndClear(5)//
                .assertNoTerminalEvent()//
                .requestMore(1)//
                .assertValuesAndClear(6)//
                .assertNoTerminalEvent()//
                .requestMore(1)//
                .assertValuesAndClear(7)//
                .requestMore(1) //
                .assertNoValues().assertCompleted();
    }

    @Test
    public void testUnsubscribe() {
        Observable<Integer> a = Observable.just(1, 2, 5, 7, 6, 8);
        Observable<Integer> b = Observable.just(3, 4, 5, 6, 7);
        final List<Integer> list = new ArrayList<Integer>();
        final AtomicBoolean terminal = new AtomicBoolean();
        matchThem(a, b).subscribe(new Subscriber<Integer>() {

            @Override
            public void onCompleted() {
                terminal.set(true);
            }

            @Override
            public void onError(Throwable e) {
                terminal.set(true);
            }

            @Override
            public void onNext(Integer t) {
                list.add(t);
                unsubscribe();
            }}); 
        assertFalse(terminal.get());
        assertEquals(Arrays.asList(5), list);
    }

    @Test
    public void testError() {
        RuntimeException e = new RuntimeException();
        Observable<Integer> a = Observable.just(1, 2).concatWith(Observable.<Integer> error(e));
        Observable<Integer> b = Observable.just(1, 2, 3);
        match(a, b).assertNoValues().assertError(e);
    }

    @Test(timeout = 5000)
    public void testOneDoesNotCompleteAndOtherMatchedAllShouldFinish() {
        Observable<Integer> a = Observable.just(1, 2).concatWith(Observable.<Integer> never());
        Observable<Integer> b = Observable.just(1, 2);
        match(a, b).assertValues(1, 2).assertCompleted();
    }

    @Test(timeout = 5000)
    public void testOneDoesNotCompleteAndOtherMatchedAllShouldFinishSwitched() {
        Observable<Integer> a = Observable.just(1, 2);
        Observable<Integer> b = Observable.just(1, 2).concatWith(Observable.<Integer> never());
        match(a, b).assertValues(1, 2).assertCompleted();
    }

    @Test
    public void testOneDoesNotCompleteAndOtherMatchedAllShouldFinishSwitched2() {
        Observable<Integer> a = Observable.just(1, 2);
        Observable<Integer> b = Observable.just(1, 3).concatWith(Observable.<Integer> never());
        match(a, b).assertValues(1).assertNoTerminalEvent();
    }

    @Test
    public void testLongReversed() {
        for (int n = 1; n < 1000; n++) {
            final int N = n;
            Observable<Integer> a = Observable.range(1, n).map(new Func1<Integer, Integer>() {
                @Override
                public Integer call(Integer x) {
                    return N + 1 - x;
                }
            });
            Observable<Integer> b = Observable.range(1, n);
            boolean equals = Observable
                    .sequenceEqual(matchThem(a, b).sorted(), Observable.range(1, n)).toBlocking()
                    .single();
            assertTrue(equals);
        }
    }

    @Test
    public void testLongShifted() {
        for (int n = 1; n < 1000; n++) {
            testShifted(n);
        }
    }

    @Test
    public void testVeryLongShifted() {
        testShifted(1000000);
    }

    private void testShifted(int n) {
        Observable<Integer> a = Observable.just(0).concatWith(Observable.range(1, n));
        Observable<Integer> b = Observable.range(1, n);
        assertTrue(Observable
                .sequenceEqual(a.compose(Transformers.matchWith(b, Functions.identity(),
                        Functions.identity(), COMBINER)), Observable.range(1, n))
                .toBlocking().single());
    }

    private static Observable<Integer> matchThem(Observable<Integer> a, Observable<Integer> b) {
        return a.compose(
                Transformers.matchWith(b, Functions.identity(), Functions.identity(), COMBINER));
    }

    private static void match(Observable<Integer> a, Observable<Integer> b, Integer... expected) {
        List<Integer> list = Arrays.asList(expected);
        TestSubscriber2<Integer> ts = match(a, b).assertCompleted();
        assertEquals(list, ts.getOnNextEvents());
    }

    private static TestSubscriber2<Integer> match(Observable<Integer> a, Observable<Integer> b) {
        return matchThem(a, b).to(TestingHelper.<Integer> test());
    }

    private static final Func2<Integer, Integer, Integer> COMBINER = new Func2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer x, Integer y) {
            return x;
        }
    };

}