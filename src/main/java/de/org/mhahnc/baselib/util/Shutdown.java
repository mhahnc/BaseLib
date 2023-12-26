package de.org.mhahnc.baselib.util;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shutdown handler, so a program can query if a shutdown has been activated.
 * The actual shutdown will be delayed until the handler gets release, which
 * allows for proper termination of the whole process. no matter how long it
 * will take.
 */
public class Shutdown {
    static Shutdown      _lock = new Shutdown();
    static Thread        _thrd;
    static AtomicBoolean _active = new AtomicBoolean();

    /**
     * Call this at the beginning of a process' lifetime. From this point on
     * a shutdown will be delayed until the release() method gets called.
     * @param name Name the shutdown thread should carry.
     * @return True if the shutdown handler got already installed, false if not.
     */
    public static boolean install(String name) {
        synchronized(_lock) {
            if (null == _thrd) {
                _thrd = new Thread(name) {
                    public void run() {
                        synchronized(_lock) {
                            if (_active.get()) {
                                return;
                            }
                            _active.set(true);
                            try {
                                _lock.wait();
                            }
                            catch (InterruptedException ignored) {
                            }
                        }
                    }
                };
                Runtime.getRuntime().addShutdownHook(_thrd);
                return true;
            }
            return false;
        }
    }

    /**
     * To check if a shutdown is going on. The owning process should poll on a
     * regular interval or in appropriate places to make sure such a break gets
     * noticed and proper exit measures are taken.
     * @return True if a shutdown is going on.
     */
    public static boolean active() {
        return _active.get();
    }

    /**
     * To be called once at the end of process execution, when everything has
     * been cleaned up and the system is completely ready for termination.
     */
    public static void release() {
        if (null != _thrd) {
            synchronized(_lock) {
                _active.set(true);
                _lock.notifyAll();
                try {
                    Runtime.getRuntime().removeShutdownHook(_thrd);
                }
                catch (IllegalStateException couldHappen) {
                }
            }
        }
    }

    /**
     * Resets every to startup state. Primarily useful for test cases, since
     * it counts on everything to be stopped.
     * @return Zero if the thread was still active. 1 if cleanup all worked.
     * 2 if the hook wasn't removed.
     */
    public static int reset() {
        if (null == _thrd) {
            return 0;
        }
        int result = 1;
        result += Runtime.getRuntime().removeShutdownHook(_thrd) ? 1 : 0;
        _thrd = null;
        _active.set(false);
        return result;
    }
}
