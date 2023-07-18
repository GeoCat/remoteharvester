package net.geocat.service;


import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Scope("singleton")
public class ProcessLockingService {

    public static HashMap<String, Lock> lockMap = new HashMap<>();

    // callers responsibility to lock and unlock the lock....
    // also, make sure you commit transactions before you return the lock so other threads will see the DB changes.
    public synchronized Lock getLock(String processId) {
        Lock lock = lockMap.get(processId);
        if (lock == null) {
            lock = new ReentrantLock();
            lockMap.put(processId, lock);
        }
        return lock;
    }

}
