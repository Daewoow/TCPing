package org.example;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;

public class TCPing {

    private static final int DEFAULT_PORT = 80;
    private static final int DEFAULT_TIMEOUT = 5000;
    private static final int DEFAULT_COUNT = 4;
    private static final int DEFAULT_INTERVAL = 1000;

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            return;
        }

        String host = args[0];
        int port = DEFAULT_PORT;

        if (args.length == 2) {
            try {
                port = Integer.parseInt(args[1]);
                if (port < 1 || port > 65535) {
                    System.err.println("Порт должен быть в диапазоне 1-65535");
                    return;
                }
            } catch (NumberFormatException e) {
                System.err.println("Неверный формат порта");
                return;
            }
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
            System.out.println("\nTCPing завершен.");
        }));

        System.out.printf("TCPing %s [%s] с портом %d:\n", host, resolveIP(host), port);

        int successCount = 0;
        int failCount = 0;
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = 0;

        for (int i = 0; i < DEFAULT_COUNT; i++) {
            try {
                Instant start = Instant.now();
                boolean reachable = isReachable(host, port);
                Duration duration = Duration.between(start, Instant.now());
                long elapsed = duration.toMillis();

                if (reachable) {
                    successCount++;
                    totalTime += elapsed;
                    minTime = Math.min(minTime, elapsed);
                    maxTime = Math.max(maxTime, elapsed);
                    System.out.printf("Ответ от %s: время=%dмс\n", resolveIP(host), elapsed);
                } else {
                    failCount++;
                    System.out.printf("Не удалось подключиться к %s за %dмс\n", host, elapsed);
                }

                if (i < DEFAULT_COUNT - 1) {
                    Thread.sleep(DEFAULT_INTERVAL);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (successCount > 0) {
            System.out.println("\nСтатистика TCPing:");
            System.out.printf("    Пакетов: отправлено = %d, получено = %d, потеряно = %d (%.0f%% потерь)\n",
                    DEFAULT_COUNT, successCount, failCount, (failCount * 100.0 / DEFAULT_COUNT));
            System.out.print("    Приблизительное время приема-передачи в мс:\n");
            System.out.printf("        Минимальное = %dмс, Максимальное = %dмс, Среднее = %.0fмс\n",
                    minTime, maxTime, (double) totalTime / successCount);
        }

        executor.shutdown();
    }

    private static boolean isReachable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), DEFAULT_TIMEOUT);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String resolveIP(String host) {
        try {
            return java.net.InetAddress.getByName(host).getHostAddress();
        } catch (java.net.UnknownHostException e) {
            return "неизвестный адрес";
        }
    }
}