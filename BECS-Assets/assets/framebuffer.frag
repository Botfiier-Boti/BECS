#version 330 core

out vec4 fragColor;
in vec2 TexCoords;
uniform sampler2D fbo_texture;

const float size = 1.0 / 256.0;

void main()
{ 
	vec4 col = texture2D(fbo_texture, TexCoords);
	if (col.a > 0.5)
		fragColor = col;
	else {
		float a = texture2D(fbo_texture, vec2(TexCoords.x + size, TexCoords.y)).a +
				  texture2D(fbo_texture, vec2(TexCoords.x, TexCoords.y - size)).a +
				  texture2D(fbo_texture, vec2(TexCoords.x - size, TexCoords.y)).a +
				  texture2D(fbo_texture, vec2(TexCoords.x, TexCoords.y + size)).a;
		if (col.a < 1.0 && a > 0) 
			fragColor = vec4(1, 1, 1, 0.8);
		else
			fragColor = col;
	}
}