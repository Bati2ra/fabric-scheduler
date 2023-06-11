package com.bati.fabricscheduler.impl;

import com.bati.fabricscheduler.Mod;
import com.bati.fabricscheduler.model.FabricRunnable;
import com.bati.fabricscheduler.model.FabricScheduler;
import com.bati.fabricscheduler.model.FabricTask;
import com.bati.fabricscheduler.model.FabricWorker;
import org.apache.commons.lang3.Validate;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class FabricSchedulerImpl implements FabricScheduler {

    /**
     * Counter for IDs. Order doesn't matter, only uniqueness.
     */
    private final AtomicInteger ids = new AtomicInteger(1);
    /**
     * Current head of linked-list. This reference is always stale, {@link FabricTaskImpl#next} is the live reference.
     */
    private volatile FabricTaskImpl head = new FabricTaskImpl();
    /**
     * Tail of a linked-list. AtomicReference only matters when adding to queue
     */
    private final AtomicReference<FabricTaskImpl> tail = new AtomicReference<FabricTaskImpl>(head);
    /**
     * Main thread logic only
     */
    private final PriorityQueue<FabricTaskImpl> pending = new PriorityQueue<FabricTaskImpl>(10,
            new Comparator<FabricTaskImpl>() {
                public int compare(final FabricTaskImpl o1, final FabricTaskImpl o2) {
                    return (int) (o1.getNextRun() - o2.getNextRun());
                }
            });
    /**
     * Main thread logic only
     */
    private final List<FabricTaskImpl> temp = new ArrayList<>();
    /**
     * These are tasks that are currently active. It's provided for 'viewing' the current state.
     */
    private final ConcurrentHashMap<Integer, FabricTaskImpl> runners = new ConcurrentHashMap<Integer, FabricTaskImpl>();
    private volatile int currentTick = -1;
    private final Executor executor = Executors.newCachedThreadPool();
    private FabricAsyncDebugger debugHead = new FabricAsyncDebugger(-1, null, null) {@Override StringBuilder debugTo(StringBuilder string) {return string;}};
    private FabricAsyncDebugger debugTail = debugHead;
    private static final int RECENT_TICKS;

    static {
        RECENT_TICKS = 30;
    }

    public int scheduleSyncDelayedTask(final Mod mod, final Runnable task) {
        return this.scheduleSyncDelayedTask(mod, task, 0l);
    }

    public FabricTask runTask(Mod mod, Runnable runnable) {
        return runTaskLater(mod, runnable, 0l);
    }

    @Deprecated
    public int scheduleAsyncDelayedTask(final Mod mod, final Runnable task) {
        return this.scheduleAsyncDelayedTask(mod, task, 0l);
    }

    public FabricTask runTaskAsynchronously(Mod mod, Runnable runnable) {
        return runTaskLaterAsynchronously(mod, runnable, 0l);
    }

    public int scheduleSyncDelayedTask(final Mod mod, final Runnable task, final long delay) {
        return this.scheduleSyncRepeatingTask(mod, task, delay, -1l);
    }

    public FabricTask runTaskLater(Mod mod, Runnable runnable, long delay) {
        return runTaskTimer(mod, runnable, delay, -1l);
    }

    @Deprecated
    public int scheduleAsyncDelayedTask(final Mod mod, final Runnable task, final long delay) {
        return this.scheduleAsyncRepeatingTask(mod, task, delay, -1l);
    }

    public FabricTask runTaskLaterAsynchronously(Mod mod, Runnable runnable, long delay) {
        return runTaskTimerAsynchronously(mod, runnable, delay, -1l);
    }

    public int scheduleSyncRepeatingTask(final Mod mod, final Runnable runnable, long delay, long period) {
        return runTaskTimer(mod, runnable, delay, period).getTaskId();
    }

    public FabricTask runTaskTimer(Mod mod, Runnable runnable, long delay, long period) {
        validate(mod, runnable);
        if (delay < 0l) {
            delay = 0;
        }
        if (period == 0l) {
            period = 1l;
        } else if (period < -1l) {
            period = -1l;
        }
        return handle(new FabricTaskImpl(mod, runnable, nextId(), period), delay);
    }

    @Deprecated
    public int scheduleAsyncRepeatingTask(final Mod mod, final Runnable runnable, long delay, long period) {
        return runTaskTimerAsynchronously(mod, runnable, delay, period).getTaskId();
    }

    public FabricTask runTaskTimerAsynchronously(Mod mod, Runnable runnable, long delay, long period) {
        validate(mod, runnable);
        if (delay < 0l) {
            delay = 0;
        }
        if (period == 0l) {
            period = 1l;
        } else if (period < -1l) {
            period = -1l;
        }
        return handle(new FabricAsyncTask(runners, mod, runnable, nextId(), period), delay);
    }

    public <T> Future<T> callSyncMethod(final Mod mod, final Callable<T> task) {
        validate(mod, task);
        final FabricFuture<T> future = new FabricFuture<>(task, mod, nextId());
        handle(future, 0l);
        return future;
    }

    public void cancelTask(final int taskId) {
        if (taskId <= 0) {
            return;
        }
        FabricTaskImpl task = runners.get(taskId);
        if (task != null) {
            task.cancel0();
        }
        task = new FabricTaskImpl(
                new Runnable() {
                    public void run() {
                        if (!check(FabricSchedulerImpl.this.temp)) {
                            check(FabricSchedulerImpl.this.pending);
                        }
                    }
                    private boolean check(final Iterable<FabricTaskImpl> collection) {
                        final Iterator<FabricTaskImpl> tasks = collection.iterator();
                        while (tasks.hasNext()) {
                            final FabricTaskImpl task = tasks.next();
                            if (task.getTaskId() == taskId) {
                                task.cancel0();
                                tasks.remove();
                                if (task.isSync()) {
                                    runners.remove(taskId);
                                }
                                return true;
                            }
                        }
                        return false;
                    }});
        handle(task, 0l);
        for (FabricTaskImpl taskPending = head.getNext(); taskPending != null; taskPending = taskPending.getNext()) {
            if (taskPending == task) {
                return;
            }
            if (taskPending.getTaskId() == taskId) {
                taskPending.cancel0();
            }
        }
    }

    public void cancelTasks(final Mod mod) {
        Validate.notNull(mod, "Cannot cancel tasks of null mod");
        final FabricTaskImpl task = new FabricTaskImpl(
                new Runnable() {
                    public void run() {
                        check(FabricSchedulerImpl.this.pending);
                        check(FabricSchedulerImpl.this.temp);
                    }
                    void check(final Iterable<FabricTaskImpl> collection) {
                        final Iterator<FabricTaskImpl> tasks = collection.iterator();
                        while (tasks.hasNext()) {
                            final FabricTaskImpl task = tasks.next();
                            if (task.getOwner().equals(mod)) {
                                task.cancel0();
                                tasks.remove();
                                if (task.isSync()) {
                                    runners.remove(task.getTaskId());
                                }
                            }
                        }
                    }
                });
        handle(task, 0l);
        for (FabricTaskImpl taskPending = head.getNext(); taskPending != null; taskPending = taskPending.getNext()) {
            if (taskPending == task) {
                return;
            }
            if (taskPending.getTaskId() != -1 && taskPending.getOwner().equals(mod)) {
                taskPending.cancel0();
            }
        }
        for (FabricTaskImpl runner : runners.values()) {
            if (runner.getOwner().equals(mod)) {
                runner.cancel0();
            }
        }
    }

    public void cancelAllTasks() {
        final FabricTaskImpl task = new FabricTaskImpl(
                new Runnable() {
                    public void run() {
                        Iterator<FabricTaskImpl> it = FabricSchedulerImpl.this.runners.values().iterator();
                        while (it.hasNext()) {
                            FabricTaskImpl task = it.next();
                            task.cancel0();
                            if (task.isSync()) {
                                it.remove();
                            }
                        }
                        FabricSchedulerImpl.this.pending.clear();
                        FabricSchedulerImpl.this.temp.clear();
                    }
                });
        handle(task, 0l);
        for (FabricTaskImpl taskPending = head.getNext(); taskPending != null; taskPending = taskPending.getNext()) {
            if (taskPending == task) {
                break;
            }
            taskPending.cancel0();
        }
        for (FabricTaskImpl runner : runners.values()) {
            runner.cancel0();
        }
    }

    public boolean isCurrentlyRunning(final int taskId) {
        final FabricTaskImpl task = runners.get(taskId);
        if (task == null || task.isSync()) {
            return false;
        }
        final FabricAsyncTask asyncTask = (FabricAsyncTask) task;
        synchronized (asyncTask.getWorkers()) {
            return asyncTask.getWorkers().isEmpty();
        }
    }

    public boolean isQueued(final int taskId) {
        if (taskId <= 0) {
            return false;
        }
        for (FabricTaskImpl task = head.getNext(); task != null; task = task.getNext()) {
            if (task.getTaskId() == taskId) {
                return task.getPeriod() >= -1l; // The task will run
            }
        }
        FabricTaskImpl task = runners.get(taskId);
        return task != null && task.getPeriod() >= -1l;
    }

    public List<FabricWorker> getActiveWorkers() {
        final ArrayList<FabricWorker> workers = new ArrayList<FabricWorker>();
        for (final FabricTaskImpl taskObj : runners.values()) {
            // Iterator will be a best-effort (may fail to grab very new values) if called from an async thread
            if (taskObj.isSync()) {
                continue;
            }
            final FabricAsyncTask task = (FabricAsyncTask) taskObj;
            synchronized (task.getWorkers()) {
                // This will never have an issue with stale threads; it's state-safe
                workers.addAll(task.getWorkers());
            }
        }
        return workers;
    }

    public List<FabricTask> getPendingTasks() {
        final ArrayList<FabricTaskImpl> truePending = new ArrayList<FabricTaskImpl>();
        for (FabricTaskImpl task = head.getNext(); task != null; task = task.getNext()) {
            if (task.getTaskId() != -1) {
                // -1 is special code
                truePending.add(task);
            }
        }

        final ArrayList<FabricTask> pending = new ArrayList<FabricTask>();
        for (FabricTaskImpl task : runners.values()) {
            if (task.getPeriod() >= -1l) {
                pending.add(task);
            }
        }

        for (final FabricTaskImpl task : truePending) {
            if (task.getPeriod() >= -1l && !pending.contains(task)) {
                pending.add(task);
            }
        }
        return pending;
    }

    /**
     * This method is designed to never block or wait for locks; an immediate execution of all current tasks.
     */
    public void mainThreadHeartbeat(final int currentTick) {
        this.currentTick = currentTick;
        final List<FabricTaskImpl> temp = this.temp;
        parsePending();
        while (isReady(currentTick)) {
            final FabricTaskImpl task = pending.remove();
            if (task.getPeriod() < -1l) {
                if (task.isSync()) {
                    runners.remove(task.getTaskId(), task);
                }
                parsePending();
                continue;
            }
            if (task.isSync()) {
                try {
                    task.run();
                } catch (final Throwable throwable) {
                    task.getOwner().getLogger().log(
                            Level.WARNING,
                            String.format(
                                    "Task #%s for %s generated an exception",
                                    task.getTaskId(),
                                    task.getOwner().getName()),
                            throwable);
                }
                parsePending();
            } else {
                debugTail = debugTail.setNext(new FabricAsyncDebugger(currentTick + RECENT_TICKS, task.getOwner(), task.getTaskClass()));
                executor.execute(task);
                // We don't need to parse pending
                // (async tasks must live with race-conditions if they attempt to cancel between these few lines of code)
            }
            final long period = task.getPeriod(); // State consistency
            if (period > 0) {
                task.setNextRun(currentTick + period);
                temp.add(task);
            } else if (task.isSync()) {
                runners.remove(task.getTaskId());
            }
        }
        pending.addAll(temp);
        temp.clear();
        debugHead = debugHead.getNextHead(currentTick);
    }

    private void addTask(final FabricTaskImpl task) {
        final AtomicReference<FabricTaskImpl> tail = this.tail;
        FabricTaskImpl tailTask = tail.get();
        while (!tail.compareAndSet(tailTask, task)) {
            tailTask = tail.get();
        }
        tailTask.setNext(task);
    }

    private FabricTaskImpl handle(final FabricTaskImpl task, final long delay) {
        task.setNextRun(currentTick + delay);
        addTask(task);
        return task;
    }

    private static void validate(final Mod mod, final Object task) {
        Validate.notNull(mod, "Mod cannot be null");
        Validate.notNull(task, "Task cannot be null");
        if (!mod.isEnabled()) {
            //throw new IllegalModAccessException("Mod attempted to register task while disabled");
            throw new RuntimeException("Mod attempted to register task while disabled");
        }
    }

    private int nextId() {
        return ids.incrementAndGet();
    }

    private void parsePending() {
        FabricTaskImpl head = this.head;
        FabricTaskImpl task = head.getNext();
        FabricTaskImpl lastTask = head;
        for (; task != null; task = (lastTask = task).getNext()) {
            if (task.getTaskId() == -1) {
                task.run();
            } else if (task.getPeriod() >= -1l) {
                pending.add(task);
                runners.put(task.getTaskId(), task);
            }
        }
        // We split this because of the way things are ordered for all of the async calls in CraftScheduler
        // (it prevents race-conditions)
        for (task = head; task != lastTask; task = head) {
            head = task.getNext();
            task.setNext(null);
        }
        this.head = lastTask;
    }

    private boolean isReady(final int currentTick) {
        return !pending.isEmpty() && pending.peek().getNextRun() <= currentTick;
    }

    @Override
    public String toString() {
        int debugTick = currentTick;
        StringBuilder string = new StringBuilder("Recent tasks from ").append(debugTick - RECENT_TICKS).append('-').append(debugTick).append('{');
        debugHead.debugTo(string);
        return string.append('}').toString();
    }

    @Deprecated
    @Override
    public int scheduleSyncDelayedTask(Mod mod, FabricRunnable task, long delay) {
        return scheduleSyncDelayedTask(mod, (Runnable) task, delay);
    }

    @Deprecated
    @Override
    public int scheduleSyncDelayedTask(Mod mod, FabricRunnable task) {
        return scheduleSyncDelayedTask(mod, (Runnable) task);
    }

    @Deprecated
    @Override
    public int scheduleSyncRepeatingTask(Mod mod, FabricRunnable task, long delay, long period) {
        return scheduleSyncRepeatingTask(mod, (Runnable) task, delay, period);
    }

    @Deprecated
    @Override
    public FabricTask runTask(Mod mod, FabricRunnable task) throws IllegalArgumentException {
        return runTask(mod, (Runnable) task);
    }

    @Deprecated
    @Override
    public FabricTask runTaskAsynchronously(Mod mod, FabricRunnable task) throws IllegalArgumentException {
        return runTaskAsynchronously(mod, (Runnable) task);
    }

    @Deprecated
    @Override
    public FabricTask runTaskLater(Mod mod, FabricRunnable task, long delay) throws IllegalArgumentException {
        return runTaskLater(mod, (Runnable) task, delay);
    }

    @Deprecated
    @Override
    public FabricTask runTaskLaterAsynchronously(Mod mod, FabricRunnable task, long delay) throws IllegalArgumentException {
        return runTaskLaterAsynchronously(mod, (Runnable) task, delay);
    }

    @Deprecated
    @Override
    public FabricTask runTaskTimer(Mod mod, FabricRunnable task, long delay, long period) throws IllegalArgumentException {
        return runTaskTimer(mod, (Runnable) task, delay, period);
    }

    @Deprecated
    @Override
    public FabricTask runTaskTimerAsynchronously(Mod mod, FabricRunnable task, long delay, long period) throws IllegalArgumentException {
        return runTaskTimerAsynchronously(mod, (Runnable) task, delay, period);
    }
}