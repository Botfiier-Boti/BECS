package com.botifier.becs.sound;

import static org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
import static org.lwjgl.openal.AL10.AL_FORMAT_STEREO16;
import static org.lwjgl.openal.AL10.alBufferData;
import static org.lwjgl.openal.AL10.alDeleteBuffers;
import static org.lwjgl.openal.AL10.alGenBuffers;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_close;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_get_info;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_get_samples_short_interleaved;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_open_memory;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_stream_length_in_samples;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBVorbisInfo;

import com.botifier.becs.util.IOUtil;

/**https://lwjglgamedev.gitbooks.io/3d-game-development-with-lwjgl/content/chapter22/chapter22.html modified
 * SoundBuffer
 * 
 * TODO: Replace this?
**/
public class SoundBuffer {

	private final int bufferId;

	public SoundBuffer(String file) {
		this.bufferId = alGenBuffers();
		if (file.endsWith(".ogg")) {
			try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
				ShortBuffer pcm = readVorbis(file, 32 * 1024, info);

				alBufferData(bufferId, info.channels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, pcm, info.sample_rate());
			}
		} else if (file.endsWith(".wav")) {
			try {
				ByteBuffer pcm = readWav(file);
				alBufferData(bufferId, AL_FORMAT_STEREO16, pcm, 44100);
			} catch (IOException e) {
				throw new RuntimeException("Failed to load WAV: "+file, e); 
			}
		}

	}

	public int getBufferId() {
		return this.bufferId;
	}

	public void cleanup() {
		alDeleteBuffers(this.bufferId);
	}

	static ShortBuffer readVorbis(String resource, int bufferSize, STBVorbisInfo info) {
        ByteBuffer vorbis;
        try {
            vorbis = IOUtil.ioResourceToByteBuffer(resource, bufferSize);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        IntBuffer error   = BufferUtils.createIntBuffer(1);
        long      decoder = stb_vorbis_open_memory(vorbis, error, null);
        if (decoder == NULL) {
            throw new RuntimeException("Failed to open Ogg Vorbis file. Error: " + error.get(0)); 
        }

        stb_vorbis_get_info(decoder, info);

        int channels = info.channels();

        ShortBuffer pcm = BufferUtils.createShortBuffer(stb_vorbis_stream_length_in_samples(decoder) * channels);

        stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);
        stb_vorbis_close(decoder);

        return pcm;
    }

	static ByteBuffer readWav(String resource) throws IOException {
		try (InputStream is = SoundBuffer.class.getClassLoader().getResourceAsStream(resource)) {
			BufferedInputStream bis = new BufferedInputStream(is);
			AudioInputStream ais = AudioSystem.getAudioInputStream(bis);

			AudioFormat format = ais.getFormat();

			if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
				throw new IllegalArgumentException("Only PCM WAV files are supported"); 
			}

			int bufferSize = (int) (ais.getFrameLength() * format.getFrameSize());

			ByteBuffer data = BufferUtils.createByteBuffer(bufferSize);

			data.put(ais.readAllBytes());
			data.flip();

			return data;

		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
		return null;
	}
}
