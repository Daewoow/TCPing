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
            printHelp();
            return;
        }

        String host = args[0];
        if (args[0].equals("--h")) {
            printHelp();
        }
        var port = DEFAULT_PORT;

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

        var successCount = 0;
        var failCount = 0;
        var totalTime = 0L;
        var minTime = Long.MAX_VALUE;
        var maxTime = 0L;

        for (int i = 0; i < DEFAULT_COUNT; i++) {
            try {
                Instant start = Instant.now();
                var reachable = isReachable(host, port);
                Duration duration = Duration.between(start, Instant.now());
                var elapsed = duration.toMillis();

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

    private static void printHelp() {
        System.out.println("Использование: tcping <хост> [порт]");
        System.out.println("Примеры:");
        System.out.println("  tcping example.ru       # Проверка порта 80");
        System.out.println("  tcping example.ru 443   # Проверка порта 443");
    }
}