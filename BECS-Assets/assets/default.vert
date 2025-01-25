#version 430
#pragma optimize(off)

layout(location = 0) in vec3 position;
layout(location = 1) in int color;
layout(location = 2) in vec2 texcoord;
layout(location = 3) in mat3 transform;

out vec4 vertexColor;
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
    vertexColor = unpackColor(color).rgba;
    textureCoord = texcoord;
    mat4 mvp = projection * view * model;
    gl_Position = mvp * vec4(position, 1.0);
}