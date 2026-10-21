package com.example.bank;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.*;
import static org.junit.jupiter.api.Assertions.*;

/** 真正启动 Main 子进程；覆盖配置只存在于临时类路径，不写入原 db.properties。 */
class MainSmokeTest extends BankTestSupport {
    @TempDir Path config;

    @Test void mainOpensDepositsWithdrawsAndQueries() throws Exception {
        try (var out = Files.newOutputStream(config.resolve("db.properties"))) { properties.store(out,"Test-owned database"); }
        String cp = config + File.pathSeparator + System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),
                "-cp", cp, "org.example.Main").redirectErrorStream(true).start();
        StringBuilder transcript = new StringBuilder();
        AtomicReference<String> number = new AtomicReference<>();
        CountDownLatch opened = new CountDownLatch(1);
        ExecutorService reader = Executors.newSingleThreadExecutor();
        Future<?> capture = reader.submit(() -> {
            try (var lines = new BufferedReader(new InputStreamReader(child.getInputStream(), StandardCharsets.UTF_8))) {
                for (String line; (line = lines.readLine()) != null;) {
                    transcript.append(line).append('\n');
                    var match = Pattern.compile("开户成功，账号：(BA[0-9a-f]{32})").matcher(line);
                    if (match.find()) { number.set(match.group(1)); opened.countDown(); }
                }
            } catch (IOException e) { throw new UncheckedIOException(e); }
        });
        try (var input = new OutputStreamWriter(child.getOutputStream(), StandardCharsets.UTF_8)) {
            input.write("8\n\n完整演示客户\n13800138000\n"); input.flush();
            if (!opened.await(15, TimeUnit.SECONDS)) {
                child.destroyForcibly(); capture.get(5, TimeUnit.SECONDS);
                fail("Main 未完成开户："+transcript);
            }
            String no = number.get();
            input.write("9\n"+no+"\n100.50\n10\n"+no+"\n20.25\n11\n"+no+"\n12\n"+no+"\n1\n10\n0\n"); input.flush();
            assertTrue(child.waitFor(20, TimeUnit.SECONDS)); capture.get(5, TimeUnit.SECONDS);
            assertEquals(0, child.exitValue(), transcript.toString());
            assertTrue(transcript.toString().contains("balance=80.25"),transcript.toString());
            assertTrue(transcript.toString().contains("流水总数：2"));
            assertTrue(transcript.toString().contains("Parameters:")); assertTrue(transcript.toString().contains("Total:"));
            Files.createDirectories(Path.of("target"));
            Files.writeString(Path.of("target/bank-main-demo.log"),transcript);
        } finally {
            if (child.isAlive()) child.destroyForcibly();
            reader.shutdownNow(); assertTrue(reader.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
