package com.botifier.becs.sound;

import static org.lwjgl.openal.AL10.AL_ORIENTATION;
import static org.lwjgl.openal.AL10.AL_POSITION;
import static org.lwjgl.openal.AL10.AL_VELOCITY;
import static org.lwjgl.openal.AL10.alListener3f;
import static org.lwjgl.openal.AL10.alListenerfv;

import org.joml.Vector3f;

/**https://lwjglgamedev.gitbooks.io/3d-game-development-with-lwjgl/content/chapter22/chapter22.html
 * SoundListener
 * 
 * TODO: Replace this?
 */
public class SoundListener {

	public SoundListener() {
		this(new Vector3f(0, 0, 0));
	}

	public SoundListener(Vector3f position) {
	     alListener3f(AL_POSITION, position.x, position.y, position.z);
	     alListener3f(AL_VELOCITY, 0, 0, 0);
	}

	public void setSpeed(Vector3f speed) {
        alListener3f(AL_VELOCITY, speed.x, speed.y, speed.z);
    }

    public void setPosition(Vector3f position) {
        alListener3f(AL_POSITION, position.x, position.y, position.z);
    }

    public void setOrientation(Vector3f at, Vector3f up) {
        float[] data = new float[6];
        data[0] = at.x;
        data[1] = at.y;
        data[2] = at.z;
        data[3] = up.x;
        data[4] = up.y;
        data[5] = up.z;
        alListenerfv(AL_ORIENTATION, data);
    }

}
