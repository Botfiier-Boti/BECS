#version 330 core

out vec4 fragColor;
in vec2 TexCoords;
uniform sampler2D fbo_texture;

void main()
{ 
	fragColor = texture2D(fbo_texture, TexCoords);
}