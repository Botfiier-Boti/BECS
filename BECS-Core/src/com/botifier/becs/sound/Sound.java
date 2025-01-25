package com.botifier.becs.sound;

public class Sound {

	private SoundSource s;
	private SoundBuffer sb;

	private Sound(SoundBuffer b, boolean loop, boolean relative) {
		sb = b;
		s = new SoundSource(loop, relative);
		s.setBuffer(b.getBufferId());
	}

	public void play() {
		s.play();
	}

	public Sound playCopy() {
		Sound s = copy();
		s.play();
		return s;
	}

	public void pause() {
		s.pause();
	}

	public void stop() {
		s.stop();
	}

	public void destroy() {
		s.cleanup();
	}

	public boolean isPlaying() {
		return s.isPlaying();
	}

	public void setVolume(float volume) {
		s.setGain(volume);
	}

	public Sound copy() {
		return new Sound(sb, s.loop, s.relative);
	}

	public static Sound createSound(String file, boolean loop, boolean relative) {
		SoundBuffer sb = new SoundBuffer(file);
		Sound s = new Sound(sb, loop, relative);
		return s;
	}

	public static Sound createSound(String file) {
		return createSound(file, false, false);
	}
}
