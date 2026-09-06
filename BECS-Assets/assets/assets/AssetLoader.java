package assets;

import java.lang.ClassLoader;
import java.io.InputStream;

public class AssetLoader {
	
	public static InputStream loadAsset(String location) {
		ClassLoader cl = AssetLoader.class.getClassLoader();
		return cl.getResourceAsStream(location);
	}
	
}
