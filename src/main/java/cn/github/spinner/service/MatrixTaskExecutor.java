package cn.github.spinner.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service(Service.Level.APP)
public final class MatrixTaskExecutor implements Disposable {
    private static final int MAX_CONCURRENT_TASKS = 8;
    private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger();
    private final ThreadPoolExecutor executor;

    public MatrixTaskExecutor() {
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable,
                    "spinner-matrix-call-" + THREAD_SEQUENCE.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        executor = new ThreadPoolExecutor(
                0,
                MAX_CONCURRENT_TASKS,
                30,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    public static @NotNull MatrixTaskExecutor getInstance() {
        return ApplicationManager.getApplication().getService(MatrixTaskExecutor.class);
    }

    public <T> @NotNull Future<T> submit(@NotNull Callable<T> task) {
        return executor.submit(task);
    }

    @Override
    public void dispose() {
        executor.shutdownNow();
    }
}
