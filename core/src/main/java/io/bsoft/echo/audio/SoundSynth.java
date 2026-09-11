package io.bsoft.echo.audio;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Generates the placeholder sound effects as 16-bit mono WAV data. Pure Java, no LibGDX,
 * so it can run in a build tool. Echo sounds share a detuned, reverberant "temporal"
 * character (spec §41).
 */
public final class SoundSynth {

    public static final int SAMPLE_RATE = 44100;

    private SoundSynth() {
    }

    public static byte[] generate(SoundId id) {
        return switch (id) {
            case JUMP -> wav(sweep(0.16f, 320f, 640f, 0.5f, Wave.SQUARE, 0.004f, 0.12f));
            case LAND -> wav(noiseBurst(0.09f, 0.45f, 900f));
            case RECORD_START -> wav(concat(tone(0.06f, 880f, 0.45f, Wave.SINE), tone(0.10f, 1320f, 0.45f, Wave.SINE)));
            case RECORD_STOP -> wav(concat(tone(0.06f, 1320f, 0.45f, Wave.SINE), tone(0.10f, 880f, 0.45f, Wave.SINE)));
            case ECHO_CREATE -> wav(echoize(sweep(0.35f, 200f, 1400f, 0.5f, Wave.TRIANGLE, 0.01f, 0.25f), 0.08f, 4, 0.55f));
            case ECHO_COMPLETE -> wav(echoize(sweep(0.30f, 1200f, 300f, 0.4f, Wave.TRIANGLE, 0.01f, 0.2f), 0.09f, 4, 0.5f));
            case ECHO_DIE -> wav(echoize(sweep(0.25f, 500f, 80f, 0.5f, Wave.SAW, 0.005f, 0.18f), 0.06f, 3, 0.5f));
            case ECHO_LOOP -> wav(echoize(concat(tone(0.07f, 660f, 0.35f, Wave.TRIANGLE), tone(0.07f, 990f, 0.35f, Wave.TRIANGLE)), 0.08f, 3, 0.5f));
            case PLATE_PRESS -> wav(concat(tone(0.05f, 220f, 0.5f, Wave.SQUARE), tone(0.08f, 330f, 0.4f, Wave.SINE)));
            case PLATE_RELEASE -> wav(concat(tone(0.05f, 330f, 0.4f, Wave.SINE), tone(0.08f, 220f, 0.4f, Wave.SQUARE)));
            case DOOR_OPEN -> wav(mix(sweep(0.45f, 90f, 180f, 0.4f, Wave.SAW, 0.02f, 0.2f), noiseBurst(0.45f, 0.15f, 400f)));
            case DOOR_CLOSE -> wav(mix(sweep(0.35f, 180f, 70f, 0.45f, Wave.SAW, 0.01f, 0.1f), noiseBurst(0.12f, 0.3f, 600f)));
            case SWITCH -> wav(concat(tone(0.03f, 1500f, 0.4f, Wave.SQUARE), tone(0.06f, 1000f, 0.4f, Wave.SQUARE)));
            case EXIT -> wav(echoize(arpeggio(new float[] {523f, 659f, 784f, 1047f}, 0.11f, 0.4f), 0.12f, 5, 0.5f));
            case PLAYER_DIE -> wav(mix(sweep(0.5f, 400f, 60f, 0.5f, Wave.SAW, 0.005f, 0.3f), noiseBurst(0.3f, 0.35f, 300f)));
            case ENEMY_DIE -> wav(concat(noiseBurst(0.06f, 0.5f, 1500f), sweep(0.2f, 600f, 150f, 0.4f, Wave.SQUARE, 0.005f, 0.15f)));
            case REWIND -> wav(sweep(0.30f, 1600f, 200f, 0.35f, Wave.TRIANGLE, 0.005f, 0.15f));
            case PARADOX -> wav(echoize(mix(sweep(0.4f, 700f, 150f, 0.35f, Wave.SAW, 0.005f, 0.2f), sweep(0.4f, 720f, 90f, 0.3f, Wave.SQUARE, 0.005f, 0.2f)), 0.05f, 5, 0.6f));
            case ERROR -> wav(concat(tone(0.08f, 200f, 0.4f, Wave.SQUARE), tone(0.12f, 150f, 0.4f, Wave.SQUARE)));
        };
    }

    private enum Wave {
        SINE, SQUARE, TRIANGLE, SAW
    }

    private static float osc(Wave wave, float phase) {
        float p = phase - (float) Math.floor(phase);
        return switch (wave) {
            case SINE -> (float) Math.sin(p * Math.PI * 2.0);
            case SQUARE -> p < 0.5f ? 1f : -1f;
            case TRIANGLE -> 4f * Math.abs(p - 0.5f) - 1f;
            case SAW -> 2f * p - 1f;
        };
    }

    private static float envelope(int i, int n, float attack, float release) {
        float t = (float) i / SAMPLE_RATE;
        float total = (float) n / SAMPLE_RATE;
        float a = attack > 0f ? Math.min(1f, t / attack) : 1f;
        float r = release > 0f ? Math.min(1f, (total - t) / release) : 1f;
        return Math.max(0f, Math.min(a, r));
    }

    private static float[] sweep(float seconds, float f0, float f1, float volume, Wave wave, float attack,
                                 float release) {
        int n = (int) (seconds * SAMPLE_RATE);
        float[] out = new float[n];
        float phase = 0f;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float f = f0 + (f1 - f0) * t;
            phase += f / SAMPLE_RATE;
            out[i] = osc(wave, phase) * volume * envelope(i, n, attack, release);
        }
        return out;
    }

    private static float[] tone(float seconds, float freq, float volume, Wave wave) {
        return sweep(seconds, freq, freq, volume, wave, 0.003f, seconds * 0.5f);
    }

    private static float[] arpeggio(float[] freqs, float noteSeconds, float volume) {
        float[] out = new float[0];
        for (float f : freqs) {
            out = concat(out, tone(noteSeconds, f, volume, Wave.TRIANGLE));
        }
        return out;
    }

    private static float[] noiseBurst(float seconds, float volume, float cutoffHz) {
        int n = (int) (seconds * SAMPLE_RATE);
        float[] out = new float[n];
        long seed = 0x9E3779B97F4A7C15L;
        float lp = 0f;
        float alpha = (float) (1.0 - Math.exp(-2.0 * Math.PI * cutoffHz / SAMPLE_RATE));
        for (int i = 0; i < n; i++) {
            seed ^= seed << 13;
            seed ^= seed >>> 7;
            seed ^= seed << 17;
            float white = ((seed >>> 11) & 0xFFFF) / 32768f - 1f;
            lp += alpha * (white - lp);
            out[i] = lp * volume * envelope(i, n, 0.002f, seconds * 0.8f);
        }
        return out;
    }

    /** Adds decaying delayed copies — the signature of anything temporal. */
    private static float[] echoize(float[] src, float delaySeconds, int taps, float decay) {
        int delay = (int) (delaySeconds * SAMPLE_RATE);
        float[] out = new float[src.length + delay * taps];
        System.arraycopy(src, 0, out, 0, src.length);
        float gain = decay;
        for (int tap = 1; tap <= taps; tap++) {
            int offset = delay * tap;
            for (int i = 0; i < src.length; i++) {
                out[i + offset] += src[i] * gain;
            }
            gain *= decay;
        }
        return out;
    }

    private static float[] concat(float[] a, float[] b) {
        float[] out = new float[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static float[] mix(float[] a, float[] b) {
        float[] out = new float[Math.max(a.length, b.length)];
        for (int i = 0; i < out.length; i++) {
            float v = (i < a.length ? a[i] : 0f) + (i < b.length ? b[i] : 0f);
            out[i] = v;
        }
        return out;
    }

    private static byte[] wav(float[] samples) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int dataSize = samples.length * 2;
        ByteBuffer header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        header.put("RIFF".getBytes());
        header.putInt(36 + dataSize);
        header.put("WAVE".getBytes());
        header.put("fmt ".getBytes());
        header.putInt(16);
        header.putShort((short) 1);
        header.putShort((short) 1);
        header.putInt(SAMPLE_RATE);
        header.putInt(SAMPLE_RATE * 2);
        header.putShort((short) 2);
        header.putShort((short) 16);
        header.put("data".getBytes());
        header.putInt(dataSize);
        bytes.write(header.array(), 0, 44);
        ByteBuffer data = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN);
        for (float s : samples) {
            float clamped = Math.max(-1f, Math.min(1f, s));
            data.putShort((short) (clamped * 32767f));
        }
        bytes.write(data.array(), 0, dataSize);
        return bytes.toByteArray();
    }
}
