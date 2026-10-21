package com.example.bank;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class BankConcurrencyTest extends BankTestSupport {
    private List<Boolean> concurrent(int count, Callable<Boolean> operation) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count), start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> tasks = new ArrayList<>();
            for (int i=0; i<count; i++) tasks.add(pool.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("并发任务未启动");
                return operation.call();
            }));
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            List<Boolean> results = new ArrayList<>();
            for (var task : tasks) results.add(task.get(20, TimeUnit.SECONDS));
            return results;
        } finally {
            start.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test void concurrentDepositsDoNotLoseBalanceOrLedger() throws Exception {
        var a = bank.openAccount("并发存款", "1");
        assertEquals(8, concurrent(8, () -> { bank.deposit(a.getAccountNo(), BigDecimal.ONE); return true; }).size());
        assertEquals(new BigDecimal("8.00"), bank.account(a.getAccountNo()).getBalance());
        assertEquals(8, bank.transactions(a.getAccountNo(), 1, 20).getTotal());
    }

    @Test void concurrentWithdrawalsNeverOverdraw() throws Exception {
        var a = bank.openAccount("并发取款", "1"); bank.deposit(a.getAccountNo(), new BigDecimal("5"));
        var results = concurrent(8, () -> {
            try { bank.withdraw(a.getAccountNo(), BigDecimal.ONE); return true; }
            catch (IllegalArgumentException e) { assertEquals("余额不足", e.getMessage()); return false; }
        });
        assertEquals(5, results.stream().filter(Boolean::booleanValue).count());
        assertEquals(new BigDecimal("0.00"), bank.account(a.getAccountNo()).getBalance());
        assertEquals(6, bank.transactions(a.getAccountNo(), 1, 20).getTotal());
    }

    @Test void mixedOperationsKeepLedgerChainConsistent() throws Exception {
        var a = bank.openAccount("并发混合", "1"); bank.deposit(a.getAccountNo(), new BigDecimal("100"));
        java.util.concurrent.atomic.AtomicInteger sequence = new java.util.concurrent.atomic.AtomicInteger();
        concurrent(8, () -> {
            if (sequence.getAndIncrement() % 2 == 0) bank.deposit(a.getAccountNo(), BigDecimal.ONE);
            else bank.withdraw(a.getAccountNo(), BigDecimal.ONE);
            return true;
        });
        assertEquals(new BigDecimal("100.00"), bank.account(a.getAccountNo()).getBalance());
        var entries = new ArrayList<>(bank.transactions(a.getAccountNo(), 1, 20).getRecords());
        Collections.reverse(entries);
        BigDecimal previous = new BigDecimal("0.00");
        for (var entry : entries) { assertEquals(previous, entry.getBalanceBefore()); previous = entry.getBalanceAfter(); }
        assertEquals(new BigDecimal("100.00"), previous); assertEquals(9, entries.size());
    }
}
