package abstractdebugging;

import java.util.ArrayList;
import java.util.List;

public class EventQueue {

    private final List<Runnable> queuedEvents = new ArrayList<>();
    private final Object lock = new Object();

    /**
     * Queues a new event which will be run after the next message is sent to the client.
     * Note: Sending an event/notification also triggers running events.
     * To avoid problems any queued events should be added to the queue after all non-queued events are sent.
     */
    public void queue(Runnable runnable) {
        synchronized (lock) {
            queuedEvents.add(runnable);
        }
    }

    /**
     * Runs all queued events.
     * If a new event is queued during runAll then it will not be run during this call to runAll and will remain in the queue.
     */
    public void runAll() {
        List<Runnable> events;
        synchronized (lock) {
            events = new ArrayList<>(queuedEvents);
            queuedEvents.clear();
        }
        for (var event : events) {
            event.run();
        }
    }

}
