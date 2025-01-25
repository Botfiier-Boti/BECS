package com.botifier.becs.sound;

import static org.lwjgl.openal.AL10.AL_BUFFER;
import static org.lwjgl.openal.AL10.AL_GAIN;
import static org.lwjgl.openal.AL10.AL_LOOPING;
import static org.lwjgl.openal.AL10.AL_PLAYING;
import static org.lwjgl.openal.AL10.AL_POSITION;
import static org.lwjgl.openal.AL10.AL_SOURCE_RELATIVE;
import static org.lwjgl.openal.AL10.AL_SOURCE_STATE;
import static org.lwjgl.openal.AL10.AL_TRUE;
import static org.lwjgl.openal.AL10.AL_VELOCITY;
import static org.lwjgl.openal.AL10.alDeleteSources;
import static org.lwjgl.openal.AL10.alGenSources;
import static org.lwjgl.openal.AL10.alGetSourcei;
import static org.lwjgl.openal.AL10.alSource3f;
import static org.lwjgl.openal.AL10.alSourcePause;
import static org.lwjgl.openal.AL10.alSourcePlay;
import static org.lwjgl.openal.AL10.alSourceStop;
import static org.lwjgl.openal.AL10.alSourcef;
import static org.lwjgl.openal.AL10.alSourcei;

import org.joml.Vector3f;


//https://lwjglgamedev.gitbooks.io/3d-game-development-with-lwjgl/content/chapter22/chapter22.html
public class SoundSource {

	private final int sourceId;

	protected final boolean loop;

	protected final boolean relative;

	private int buffer;

	public SoundSource(boolean loop, boolean relative) {
		this.sourceId = alGenSources();
		this.loop = loop;
		this.relative = relative;
		if (loop) {
			alSourcei(sourceId, AL_LOOPING, AL_TRUE);
		}
		if (relative) {
			alSourcei(sourceId, AL_SOURCE_RELATIVE, AL_TRUE);
		}
	}

	public SoundSource(SoundSource s) {
		this(s.loop, s.relative);
		setBuffer(buffer);
	}

	public void setBuffer(int bufferId) {
		stop();
		buffer = bufferId;
		alSourcei(sourceId, AL_BUFFER, buffer);
	}

	public void setPosition(Vector3f position) {
		alSource3f(sourceId, AL_POSITION, position.x, position.y, position.z);
	}

	public void setSpeed(Vector3f speed) {
		alSource3f(sourceId, AL_VELOCITY, speed.x, speed.y, speed.z);
	}

	public void setGain(float gain) {
		alSourcef(sourceId, AL_GAIN, gain);
	}

	public void setProperty(int param, float value) {
		alSourcef(sourceId, param, value);
	}

	public void play() {
		alSourcePlay(sourceId);
	}

	public SoundSource playCopy() {
		SoundSource s = new SoundSource(this);
		s.play();
		return s;
	}

	public boolean isPlaying() {
		return alGetSourcei(sourceId, AL_SOURCE_STATE) == AL_PLAYING;
	}

	public void pause() {
		alSourcePause(sourceId);
	}

	public void stop() {
		alSourceStop(sourceId);
	}

	public void cleanup() {
		stop();
		alDeleteSources(sourceId);
	}

}
