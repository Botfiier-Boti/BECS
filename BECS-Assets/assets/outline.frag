#version 330

in vec3 vertexColor;
in vec2 textureCoord;
in vec2 imgSize;

out vec4 fragColor;

uniform sampler2D texImage;

const float size = 10.0 / 256.0;

void main() {
    vec4 col = texture2D(texImage, textureCoord);
    if (col.a > 0.5)
		fragColor = col;
	else {
		float a = texture2D(texImage, vec2(textureCoord.x + size, textureCoord.y)).a +
				  texture2D(texImage, vec2(textureCoord.x, textureCoord.y - size)).a +
				  texture2D(texImage, vec2(textureCoord.x - size, textureCoord.y)).a +
				  texture2D(texImage, vec2(textureCoord.x, textureCoord.y + size)).a;
		if (col.a < 1.0 && a > 0) 
			fragColor = vec4(1, 1, 1, 0.8);
		else
			fragColor = col;
	}
}