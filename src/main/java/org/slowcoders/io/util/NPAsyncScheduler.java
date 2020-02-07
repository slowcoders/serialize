package org.slowcoders.io.util;

import org.slowcoders.pal.PAL;
import org.slowcoders.util.Debug;

import java.util.ArrayDeque;
import java.util.Timer;
import java.util.TimerTask;

public class NPAsyncScheduler {

	private static final TaskList asyncTaskQ = new TaskList();
	private static final Executor executor = PAL.getAsyncExecutor();

    public interface Executor {
    	void triggerAsync();
    	boolean isInMainThread();
    	default void setTestThreadAsMainThread() {}
    }

	private static class TaskList extends ArrayDeque<UITask> implements UITask {
		public void executeTask() {
			while (true) {
				UITask task;
		        synchronized (this) {
		        	task = super.pollFirst();
		        	if (task == null) {
		                return;
		            }
		        }
				try {
					task.executeTask();
				}
				catch (Exception e) {
					throw Debug.wtf(e);
				}
			}
		}
	}

	public static void executePendingTasks() {
    	executePendingTasks(0);
	}

	public static void executePendingTasks(int timeoutInMillis) {
    	if (executor.isInMainThread()) {
			asyncTaskQ.executeTask();
		}
    	else {
			UITask trigger = new UITask() {
				@Override
				public void executeTask() throws Exception {
					synchronized (asyncTaskQ) {
						asyncTaskQ.notifyAll();
					}
				}
			};
//			new Exception().printStackTrace();
			synchronized (asyncTaskQ) {
				executeLater(trigger);
				try {
					asyncTaskQ.wait(timeoutInMillis);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}
	

    public static void executeLater(UITask task) {
        synchronized (asyncTaskQ) {
        	if (!asyncTaskQ.contains(task)) {
        		asyncTaskQ.add(task);
        	}
			if (asyncTaskQ.size() == 1) {
				executor.triggerAsync();
			}
        }
    }

	public static boolean isInMainThread() {
    	return executor.isInMainThread();
	}

    public static boolean executeInMainThread(UITask task) throws Exception {
		if (!executor.isInMainThread()) {
			executeLater(task);
			return false;
		}
		else {
			task.executeTask();
			return true;
		}
    }
    
    public static TimerTask startTimer(TimerEventHandler task, Object id, int delay) {
        TimerEvent e = new TimerEvent(task, id);
        TimerEvent.timeScheduler.schedule(e, delay, delay);
        return e;
    }

    public static class TimerEvent extends TimerTask implements UITask {
        static Timer timeScheduler = new Timer();
        TimerEventHandler task;
        Object id;

        TimerEvent(TimerEventHandler task, Object id) {
            this.task = task;
            this.id = id;
        }

        public void run() {
            NPAsyncScheduler.executeLater(this);
        }

        public void executeTask() {
            if (!task.onTimer(id)) {
                super.cancel();
            }
        }
    }

	public static void removeAllTasks() {
        synchronized (asyncTaskQ) {
        	asyncTaskQ.clear();
        }
	}

}
