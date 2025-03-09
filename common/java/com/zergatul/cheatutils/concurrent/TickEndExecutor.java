package com.zergatul.cheatutils.concurrent;

import com.zergatul.cheatutils.common.Events;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TickEndExecutor extends EventExecutor {

    public static final TickEndExecutor instance = new TickEndExecutor();

    private final List<PendingTask> tasks = new ArrayList<>();

    private TickEndExecutor() {
        super(100);
        Events.ClientTickStart.add(this::onClientTickStart);
        Events.ClientTickEnd.add(this::onClientTickEnd);
    }

    public void waitTicks(int ticks, Runnable action) {
        tasks.add(new PendingTask(ticks, action));
    }

    private void onClientTickStart() {
        for (PendingTask task : tasks) {
            task.ticks--;
        }
    }

    private void onClientTickEnd() {
        processQueue();

        for (int i = 0; i < tasks.size(); i++) {
            PendingTask task = tasks.get(i);
            if (task.ticks <= 0) {
                task.action.run();
                tasks.remove(i);
                i--;
            }
        }
    }

    private static class PendingTask {
        public int ticks;
        public final Runnable action;

        public PendingTask(int ticks, Runnable action) {
            this.ticks = ticks;
            this.action = action;
        }
    }
}