const fs = require("fs");
const path = require("path");

const sampleRate = 22050;
const outDir = path.resolve(__dirname, "../app/src/main/res/raw");
fs.mkdirSync(outDir, { recursive: true });

function wav(name, seconds, sampleFn) {
  const frames = Math.max(1, Math.floor(seconds * sampleRate));
  const data = Buffer.alloc(frames * 2);
  let seed = 0x12345678;
  const noise = () => {
    seed = (1664525 * seed + 1013904223) >>> 0;
    return (seed / 0x80000000) - 1;
  };
  for (let i = 0; i < frames; i += 1) {
    const t = i / sampleRate;
    const value = Math.max(-1, Math.min(1, sampleFn(t, i, noise)));
    data.writeInt16LE(Math.round(value * 32767), i * 2);
  }

  const header = Buffer.alloc(44);
  header.write("RIFF", 0);
  header.writeUInt32LE(36 + data.length, 4);
  header.write("WAVE", 8);
  header.write("fmt ", 12);
  header.writeUInt32LE(16, 16);
  header.writeUInt16LE(1, 20);
  header.writeUInt16LE(1, 22);
  header.writeUInt32LE(sampleRate, 24);
  header.writeUInt32LE(sampleRate * 2, 28);
  header.writeUInt16LE(2, 32);
  header.writeUInt16LE(16, 34);
  header.write("data", 36);
  header.writeUInt32LE(data.length, 40);
  fs.writeFileSync(path.join(outDir, `${name}.wav`), Buffer.concat([header, data]));
}

const sine = (hz, t) => Math.sin(Math.PI * 2 * hz * t);
const square = (hz, t) => sine(hz, t) > 0 ? 1 : -1;
const env = (t, length, attack = 0.01, release = 0.08) => {
  const a = Math.min(1, t / attack);
  const r = Math.min(1, (length - t) / release);
  return Math.max(0, Math.min(a, r));
};

wav("shoot", 0.16, (t, i, noise) => {
  const burst = Math.exp(-t * 19);
  const crack = noise() * 0.72 * burst;
  const metal = sine(940, t) * 0.25 * Math.exp(-t * 28);
  return (crack + metal) * env(t, 0.16, 0.003, 0.05);
});

wav("hit", 0.28, (t, i, noise) => {
  const ring = sine(310, t) * 0.46 * Math.exp(-t * 8) + sine(690, t) * 0.23 * Math.exp(-t * 12);
  const scrape = noise() * 0.24 * Math.exp(-t * 11);
  return (ring + scrape) * env(t, 0.28, 0.004, 0.12);
});

wav("explosion", 1.0, (t, i, noise) => {
  const rumble = sine(46 + 18 * Math.sin(t * 9), t) * 0.62 * Math.exp(-t * 2.2);
  const blast = noise() * 0.72 * Math.exp(-t * 4.1);
  const shrapnel = noise() * 0.22 * Math.exp(-t * 10);
  return (rumble + blast + shrapnel) * env(t, 1.0, 0.006, 0.32);
});

wav("powerup", 0.62, (t) => {
  const sweep = 420 + t * 980;
  const chime = sine(sweep, t) * 0.38 + sine(sweep * 1.5, t) * 0.18;
  return chime * env(t, 0.62, 0.015, 0.22);
});

wav("button", 0.12, (t) => {
  const click = sine(820, t) * 0.34 * Math.exp(-t * 34);
  const low = sine(260, t) * 0.18 * Math.exp(-t * 24);
  return (click + low) * env(t, 0.12, 0.002, 0.045);
});

wav("high_score_fanfare", 1.65, (t, i, noise) => {
  const beat = 0.165;
  const notes = [523.25, 659.25, 783.99, 1046.50, 1318.51, 1567.98, 1760.00, 2093.00];
  const step = Math.min(notes.length - 1, Math.floor(t / beat));
  const pulse = (t % beat) / beat;
  const noteEnv = Math.exp(-pulse * 2.6);
  const hz = notes[step];
  const brass = (sine(hz, t) * 0.22 + square(hz * 0.5, t) * 0.08 + sine(hz * 2, t) * 0.06) * noteEnv;
  const chord = t > 0.96 ? (sine(523.25, t) + sine(659.25, t) + sine(783.99, t)) * 0.12 * Math.exp(-(t - 0.96) * 1.6) : 0;
  const sparkle = (step % 2 === 0 ? noise() * Math.exp(-pulse * 34) * 0.09 : 0);
  return (brass + chord + sparkle) * env(t, 1.65, 0.008, 0.30);
});

wav("victory_sting", 2.35, (t, i, noise) => {
  const beat = 0.185;
  const notes = [392, 493.88, 587.33, 783.99, 987.77, 1174.66, 987.77, 783.99];
  const step = Math.min(notes.length - 1, Math.floor(t / beat));
  const pulse = (t % beat) / beat;
  const noteEnv = Math.exp(-pulse * 3.4);
  const hz = notes[step];
  const lead = (sine(hz, t) * 0.24 + sine(hz * 2, t) * 0.10) * noteEnv;
  const chordRoot = t < 1.15 ? 196 : 261.63;
  const chord = (sine(chordRoot, t) + sine(chordRoot * 1.25, t) + sine(chordRoot * 1.5, t)) * 0.075;
  const snare = (step === 3 || step === 7 ? noise() * Math.exp(-pulse * 25) * 0.16 : 0);
  const finalRing = t > 1.45 ? (sine(783.99, t) * 0.16 + sine(1174.66, t) * 0.10) * Math.exp(-(t - 1.45) * 1.8) : 0;
  return (lead + chord + snare + finalRing) * env(t, 2.35, 0.01, 0.34);
});

wav("laser", 2.02, (t, i, noise) => {
  const wobble = 72 * Math.sin(t * 17) + 25 * Math.sin(t * 41);
  const beam = sine(180 + wobble, t) * 0.36 + square(92 + wobble * 0.2, t) * 0.13;
  const sparkle = noise() * 0.08;
  return (beam + sparkle) * env(t, 2.02, 0.05, 0.25);
});

wav("music_loop", 22.0, (t, i, noise) => {
  const beat = 0.5;
  const step = Math.floor(t / beat) % 16;
  const bar = Math.floor(t / (beat * 4)) % 4;
  const bassNotes = [98, 98, 123.47, 123.47, 110, 110, 130.81, 146.83, 98, 98, 146.83, 130.81, 123.47, 110, 98, 87.31];
  const leadNotes = [392, 0, 392, 440, 493.88, 440, 392, 329.63, 349.23, 0, 349.23, 392, 440, 392, 329.63, 293.66];
  const pulse = (t % beat) / beat;
  const noteEnv = Math.exp(-pulse * 3.1);
  const bass = (square(bassNotes[step], t) * 0.12 + sine(bassNotes[step] * 0.5, t) * 0.18) * noteEnv;
  const leadHz = leadNotes[(step + bar) % leadNotes.length];
  const lead = leadHz === 0 ? 0 : (sine(leadHz, t) * 0.18 + sine(leadHz * 2, t) * 0.05) * noteEnv;
  const snare = (step % 4 === 2 ? noise() * Math.exp(-pulse * 24) * 0.22 : 0);
  const kick = (step % 4 === 0 ? sine(58, t) * Math.exp(-pulse * 20) * 0.34 : 0);
  const hat = (step % 2 === 1 ? noise() * Math.exp(-pulse * 42) * 0.08 : 0);
  return (bass + lead + snare + kick + hat) * 0.72;
});

console.log(`Generated WAV assets in ${outDir}`);
