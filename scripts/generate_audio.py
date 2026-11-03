#!/usr/bin/env python3
"""Generates all LudoMax audio (background music + sound effects) from scratch.

All sounds are synthesised here, so there are no third-party audio licences to worry about.
Requires: numpy, ffmpeg (only for the optional .ogg conversion of the music).

Usage:  python3 scripts/generate_audio.py
Output: app/src/main/res/raw/*.wav and music_loop.ogg
"""
import os, subprocess, wave
import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(42)


def save_wav(name, x, peak=0.85):
    x = np.asarray(x, dtype=np.float64)
    m = np.max(np.abs(x)) or 1.0
    x = x / m * peak
    pcm = (x * 32767).astype("<i2")
    with wave.open(os.path.join(OUT, name + ".wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def t_(dur):
    return np.arange(int(SR * dur)) / SR


def env_exp(n, decay):
    return np.exp(-np.arange(n) / SR * decay)


def fade(x, ms=4):
    n = int(SR * ms / 1000)
    x = x.copy()
    x[:n] *= np.linspace(0, 1, n)
    x[-n:] *= np.linspace(1, 0, n)
    return x


def bell(freq, dur, decay=7.0, bright=1.0):
    t = t_(dur)
    parts = [(1, 1.0), (2.01, 0.5 * bright), (3.0, 0.25 * bright), (4.2, 0.12 * bright)]
    x = sum(a * np.sin(2 * np.pi * freq * k * t) for k, a in parts)
    return fade(x * np.exp(-t * decay))


def place(buf, x, at):
    i = int(at * SR)
    n = min(len(x), len(buf) - i)
    if n > 0:
        buf[i:i + n] += x[:n]


def noise_burst(dur, decay, hp=True):
    n = int(SR * dur)
    x = rng.standard_normal(n)
    if hp:
        x = np.diff(x, prepend=0)
    return x * env_exp(n, decay)


# ------------------------------------------------------------------ SFX
def sfx_click():
    t = t_(0.06)
    return fade(np.sin(2 * np.pi * 1400 * t) * np.exp(-t * 70) + 0.4 * np.sin(2 * np.pi * 2100 * t) * np.exp(-t * 90))


def sfx_dice():
    buf = np.zeros(int(SR * 0.9))
    # clicks that slow down like a die settling
    times, tt, gap = [], 0.0, 0.035
    while tt < 0.75:
        times.append(tt)
        tt += gap
        gap *= 1.16
    for i, at in enumerate(times):
        amp = 0.5 + 0.5 * rng.random()
        k = noise_burst(0.05, 90) * amp
        th = np.sin(2 * np.pi * (180 + 60 * rng.random()) * t_(0.05)) * np.exp(-t_(0.05) * 60) * 0.7
        place(buf, k + th, at)
    return buf


def sfx_step():
    t = t_(0.09)
    f = 520 + 380 * np.exp(-t * 40)
    ph = 2 * np.pi * np.cumsum(f) / SR
    return fade(np.sin(ph) * np.exp(-t * 38))


def sfx_out():
    t = t_(0.45)
    f = 380 + 700 * (t / 0.45) ** 1.5
    ph = 2 * np.pi * np.cumsum(f) / SR
    x = (np.sin(ph) + 0.4 * np.sin(2 * ph)) * np.exp(-t * 5) * (1 - np.exp(-t * 200))
    buf = np.zeros(len(t) + int(SR * 0.1))
    place(buf, x, 0)
    place(buf, bell(1760, 0.3, 9) * 0.35, 0.22)
    return buf


def sfx_capture():
    t = t_(0.6)
    f = 760 * np.exp(-t * 4.2) + 90
    ph = 2 * np.pi * np.cumsum(f) / SR
    x = np.sign(np.sin(ph)) * 0.35 + np.sin(ph) * 0.6
    x = x * np.exp(-t * 4.5)
    x[: int(SR * 0.12)] += noise_burst(0.12, 28)[: int(SR * 0.12)] * 0.9
    return fade(x)


def sfx_home():
    buf = np.zeros(int(SR * 1.2))
    for i, f in enumerate([523.25, 659.25, 783.99, 1046.5]):
        place(buf, bell(f, 0.7, 5.5), 0.11 * i)
    return buf


def sfx_six():
    buf = np.zeros(int(SR * 0.6))
    place(buf, bell(783.99, 0.4, 8), 0.0)
    place(buf, bell(1046.5, 0.45, 7), 0.1)
    return buf


def sfx_nomove():
    buf = np.zeros(int(SR * 0.4))
    for i, f in enumerate([220, 165]):
        t = t_(0.17)
        x = (np.sign(np.sin(2 * np.pi * f * t)) * 0.5 + np.sin(2 * np.pi * f * t)) * np.exp(-t * 9)
        place(buf, fade(x), 0.17 * i)
    return buf


def sfx_win():
    buf = np.zeros(int(SR * 2.6))
    seq = [(523.25, 0.0), (659.25, 0.14), (783.99, 0.28), (1046.5, 0.42)]
    for f, at in seq:
        place(buf, bell(f, 0.5, 6), at)
    for f in (523.25, 659.25, 783.99, 1046.5, 1318.5):
        place(buf, bell(f, 1.9, 2.4, 0.8) * 0.8, 0.62)
    for i in range(8):
        place(buf, bell(1568 + 120 * i, 0.3, 11) * 0.3, 0.9 + 0.11 * i)
    return buf


# ------------------------------------------------------------------ Music
def note_freq(m):
    return 440.0 * 2 ** ((m - 69) / 12)


def synth_note(freq, dur, kind="lead", vol=1.0):
    n = int(SR * dur)
    t = np.arange(n) / SR
    vib = 1 + 0.003 * np.sin(2 * np.pi * 5.5 * t)
    ph = 2 * np.pi * freq * t * vib
    if kind == "lead":      # soft bell-ish pluck
        x = np.sin(ph) + 0.35 * np.sin(2 * ph) + 0.12 * np.sin(3 * ph)
        e = np.exp(-t * 5.5) * (1 - np.exp(-t * 400))
    elif kind == "pad":
        x = np.sin(ph) + 0.5 * np.sin(ph * 1.003) + 0.25 * np.sin(2 * ph)
        e = np.minimum(1, t * 6) * np.minimum(1, (dur - t) * 5)
    else:                   # bass
        x = np.sin(ph) + 0.4 * np.sin(2 * ph)
        e = np.exp(-t * 3.2) * (1 - np.exp(-t * 300))
    return x * e * vol


def make_music():
    bpm = 118
    beat = 60 / bpm
    bars = 16
    total = bars * 4 * beat
    tail = 2.5
    buf = np.zeros(int(SR * (total + tail)))
    # C - G - Am - F, repeated, with a lift in the last 4 bars
    chords = [
        (48, [60, 64, 67]),  # C
        (43, [59, 62, 67]),  # G
        (45, [60, 64, 69]),  # Am
        (41, [60, 65, 69]),  # F
    ]
    scale = [72, 74, 76, 79, 81, 84]  # C pentatonic C5..C6
    local = np.random.default_rng(7)
    for bar in range(bars):
        root, triad = chords[bar % 4]
        t0 = bar * 4 * beat
        # pad
        for m in triad:
            place(buf, synth_note(note_freq(m - 12), 4 * beat * 0.98, "pad", 0.10), t0)
        # bass: root on 1, fifth on 3, walking feel on 4
        place(buf, synth_note(note_freq(root), beat * 1.6, "bass", 0.55), t0)
        place(buf, synth_note(note_freq(root + 7), beat * 1.2, "bass", 0.42), t0 + 2 * beat)
        place(buf, synth_note(note_freq(root + 12), beat * 0.8, "bass", 0.3), t0 + 3.5 * beat)
        # arpeggio (eighths)
        for i in range(8):
            m = triad[[0, 1, 2, 1, 0, 1, 2, 1][i]] + 12
            place(buf, synth_note(note_freq(m), beat * 0.9, "lead", 0.16), t0 + i * beat / 2)
        # melody: only in bars 4-15, sparse and catchy
        if bar >= 4:
            pos = 0.0
            while pos < 4:
                if local.random() < 0.62:
                    near = [s for s in scale if s % 12 in {m % 12 for m in triad} or local.random() < 0.35]
                    m = int(local.choice(near if near else scale))
                    ln = float(local.choice([0.5, 0.5, 1.0]))
                    place(buf, synth_note(note_freq(m), ln * beat * 0.95, "lead", 0.34), t0 + pos * beat)
                    pos += ln
                else:
                    pos += 0.5
        # percussion
        for i in range(8):
            h = noise_burst(0.04, 140) * (0.10 if i % 2 == 0 else 0.06)
            place(buf, h, t0 + i * beat / 2)
        for b in (0, 2):
            t = t_(0.18)
            f = 120 * np.exp(-t * 14) + 45
            k = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 14) * 0.5
            place(buf, k, t0 + b * beat)
        for b in (1, 3):
            place(buf, noise_burst(0.12, 30) * 0.12, t0 + b * beat)
    # simple echo for space
    d = int(SR * beat * 0.75)
    wet = buf.copy()
    wet[d:] += buf[:-d] * 0.28
    wet[2 * d:] += buf[:-2 * d] * 0.12
    buf = wet
    # fold the tail onto the start so the loop is seamless
    n = int(SR * total)
    loop = buf[:n].copy()
    tail_part = buf[n:]
    loop[: len(tail_part)] += tail_part[: len(loop)]
    return loop


if __name__ == "__main__":
    sfx = dict(sfx_click=sfx_click, sfx_dice=sfx_dice, sfx_step=sfx_step, sfx_out=sfx_out,
               sfx_capture=sfx_capture, sfx_home=sfx_home, sfx_six=sfx_six,
               sfx_nomove=sfx_nomove, sfx_win=sfx_win)
    for name, fn in sfx.items():
        save_wav(name, fn())
        print("wrote", name)
    m = make_music()
    tmp = os.path.join(OUT, "_music_tmp.wav")
    m = m / np.max(np.abs(m)) * 0.8
    with wave.open(tmp, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((m * 32767).astype("<i2").tobytes())
    ogg = os.path.join(OUT, "music_loop.ogg")
    r = subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp, "-c:a", "libvorbis", "-q:a", "3", ogg])
    if r.returncode == 0:
        os.remove(tmp)
        print("wrote music_loop.ogg")
    else:
        os.replace(tmp, os.path.join(OUT, "music_loop.wav"))
        print("ffmpeg/libvorbis missing -> wrote music_loop.wav (rename R.raw.music_loop is unchanged)")
