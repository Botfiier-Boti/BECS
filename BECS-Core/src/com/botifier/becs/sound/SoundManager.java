package com.botifier.becs.sound;

import static org.lwjgl.openal.ALC10.alcCloseDevice;
import static org.lwjgl.openal.ALC10.alcCreateContext;
import static org.lwjgl.openal.ALC10.alcDestroyContext;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;
import static org.lwjgl.openal.ALC10.alcOpenDevice;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
//https://lwjglgamedev.gitbooks.io/3d-game-development-with-lwjgl/content/chapter22/chapter22.html
import org.lwjgl.openal.ALCapabilities;
public class SoundManager {
	private long device;

    private long context;

    private SoundListener listener;

    private final List<SoundBuffer> soundBufferList;

    private final Map<String, SoundSource> soundSourceMap;

    private final Matrix4f cameraMatrix;

    private ALCCapabilities deviceCaps;

    private ALCapabilities alCaps;

    public SoundManager() {
        soundBufferList = new ArrayList<>();
        soundSourceMap = new HashMap<>();
        cameraMatrix = new Matrix4f();
    }

    public void init() throws Exception {
        this.device = alcOpenDevice((ByteBuffer) null);
        if (device == NULL) {
            throw new IllegalStateException("Failed to open the default OpenAL device."); 
        }
        deviceCaps = ALC.createCapabilities(device);
        this.context = alcCreateContext(device, (IntBuffer) null);
        if (context == NULL) {
            throw new IllegalStateException("Failed to create OpenAL context."); 
        }
        alcMakeContextCurrent(context);
        alCaps = AL.createCapabilities(deviceCaps);
    }

    public void destroy() {
    	alcMakeContextCurrent(NULL);
    	alcDestroyContext(context);
    	alcCloseDevice(device);
    }

	public Matrix4f getCameraMatrix() {
		return cameraMatrix;
	}

	public List<SoundBuffer> getSoundBufferList() {
		return soundBufferList;
	}

	public Map<String, SoundSource> getSoundSourceMap() {
		return soundSourceMap;
	}

	public SoundListener getListener() {
		return listener;
	}

	public void setListener(SoundListener listener) {
		this.listener = listener;
	}

	public ALCapabilities getCapabilities() {
		return alCaps;
	}
}
