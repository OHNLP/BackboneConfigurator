package org.ohnlp.backbone.configurator;

import java.util.Set;
import java.util.concurrent.*;

public class WorkerService {
    private static final ExecutorService WORKER_THREAD_POOL = Executors.newWorkStealingPool();
    private static final Set<String> CURRENTLY_EXECUTING_TASKS = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<String, ConcurrentLinkedDeque<WorkerTask<?>>> CURRENTLY_SCHEDULED_TASKS = new ConcurrentHashMap<>();

    public static boolean taskIsScheduled(String taskName) {
        return CURRENTLY_SCHEDULED_TASKS.containsKey(taskName);
    }

    public static <T> CompletableFuture<T> schedule(String taskName, Callable<T> thread, boolean scheduleIfExists) {
        boolean createTask;
        CompletableFuture<T> ret = null;
        synchronized (CURRENTLY_SCHEDULED_TASKS) {
            createTask = !CURRENTLY_SCHEDULED_TASKS.containsKey(taskName);
            ConcurrentLinkedDeque<WorkerTask<?>> deque = CURRENTLY_SCHEDULED_TASKS.computeIfAbsent(taskName, k -> new ConcurrentLinkedDeque<>());
            if (createTask || scheduleIfExists) {
                ret = new CompletableFuture<>();
                deque.addLast(new WorkerTask<>(ret, thread));
            }
        }
        if (createTask) {
            WORKER_THREAD_POOL.submit(new WorkerThread(taskName));
        }
        return ret;
    }

    private static class WorkerTask<T> {
        CompletableFuture<T> taskFuture;
        Callable<T> task;

        public WorkerTask(CompletableFuture<T> taskFuture, Callable<T> task) {
            this.taskFuture = taskFuture;
            this.task = task;
        }

        public void call() {
            try {
                T ret = task.call();
                taskFuture.complete(ret);
            } catch (Throwable t) {
                taskFuture.completeExceptionally(t);
            }
        }
    }

    private static class WorkerThread implements Runnable{

        private String taskName;

        public WorkerThread(String taskName) {
            super();
            this.taskName = taskName;
        }

        @Override
        public void run() {
            while (CURRENTLY_SCHEDULED_TASKS.containsKey(taskName)) {
                // Attempt to run task, if same task is already executing, do nothing
                if (CURRENTLY_EXECUTING_TASKS.add(taskName)) {
                    // Task is not currently running
                    WorkerTask<?> toRun;
                    synchronized (CURRENTLY_SCHEDULED_TASKS) {
                        ConcurrentLinkedDeque<WorkerTask<?>> tasksForTaskName = CURRENTLY_SCHEDULED_TASKS.get(taskName);
                        if (!tasksForTaskName.isEmpty()) {
                            toRun = tasksForTaskName.removeFirst();
                        } else {
                            // There are no further tasks scheduled with this name, remove entry from schedule and exit
                            CURRENTLY_SCHEDULED_TASKS.remove(taskName);
                            return;
                        }
                    }
                    toRun.call();
                    synchronized (CURRENTLY_EXECUTING_TASKS) {
                        CURRENTLY_EXECUTING_TASKS.remove(taskName);
                        CURRENTLY_EXECUTING_TASKS.notifyAll();
                    }

                } else {
                    // An instance of this task is already running, do nothing and await changes in CURRENTLY_EXECUTING_TASKS
                    synchronized (CURRENTLY_EXECUTING_TASKS) {
                        try {
                            CURRENTLY_EXECUTING_TASKS.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }
                }

            }
        }
    }
}
