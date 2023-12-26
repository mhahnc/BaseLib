package de.org.mhahnc.baselib.test.util;

import java.io.File;

public abstract class FilePathWalker {
    public boolean walk(File path) {
        if (path.isDirectory()) {
            return walkDir(path);
        }
        return onObject(path);
    }

    private boolean walkDir(File dir) {
        if (!onObject(dir)) {
            return false;
        }
        for (File fl : dir.listFiles()) {
            if (fl.isDirectory()) {
                if (!walkDir(fl)) {
                    return false;
                }
            }
            else {
                if (!onObject(fl)) {
                    return false;
                }
            }
        }
        return true;
    }

    public abstract boolean onObject(File obj);
}
