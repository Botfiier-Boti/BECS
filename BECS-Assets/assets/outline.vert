#version 330

in vec3 position;
in int color;
in vec2 texcoord;

out vec3 vertexColor;
out vec2 textureCoord;
out vec2 imgSize;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

vec4 unpackColor(int i) {
	vec4 tempColor;
	tempColor.r = ((i >> 16) & 0xFF) / 255.0;
	tempColor.g = ((i >> 8)  & 0xFF) / 255.0;
	tempColor.b = ((i)       & 0xFF) / 255.0;
	tempColor.a = ((i >> 24) & 0xFF) / 255.0;
	return tempColor;
}

void main() {
    vertexColor = unpackColor(color).rgb;
    textureCoord = texcoord;
    mat4 mvp = projection * view * model;
    imgSize = vec2(1, 1);
    gl_Position = mvp * vec4(position, 1);
}