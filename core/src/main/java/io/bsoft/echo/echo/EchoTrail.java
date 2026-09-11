package io.bsoft.echo.echo;

/**
 * Fixed-size ring buffer of past positions for rendering afterimages (spec §32).
 * Allocation-free after construction.
 */
public final class EchoTrail {

    private final float[] xs;
    private final float[] ys;
    private final int capacity;
    private final int sampleInterval;
    private int head;
    private int size;
    private int tickCounter;

    public EchoTrail(int capacity, int sampleInterval) {
        this.capacity = capacity;
        this.sampleInterval = Math.max(1, sampleInterval);
        this.xs = new float[capacity];
        this.ys = new float[capacity];
    }

    /** Called every tick; stores a sample every {@code sampleInterval} ticks. */
    public void tick(float x, float y) {
        if (tickCounter++ % sampleInterval != 0) {
            return;
        }
        xs[head] = x;
        ys[head] = y;
        head = (head + 1) % capacity;
        if (size < capacity) {
            size++;
        }
    }

    public int size() {
        return size;
    }

    /** Sample {@code i} where 0 is the oldest and {@code size()-1} the newest. */
    public float x(int i) {
        return xs[index(i)];
    }

    public float y(int i) {
        return ys[index(i)];
    }

    private int index(int i) {
        int oldest = (head - size + capacity) % capacity;
        return (oldest + i) % capacity;
    }

    public void clear() {
        head = 0;
        size = 0;
        tickCounter = 0;
    }
}
