#version 330
#pragma optimize(off)

in vec4 vertexColor;
in vec2 textureCoord;

out vec4 fragColor;

uniform sampler2D texImage;

void main() {
	vec2 trueCoord = textureCoord;
	
    vec4 textureColor = texture(texImage, trueCoord);
    if (textureColor.a == 0)
    	discard;
    else
    	fragColor = vec4(vertexColor) * textureColor;
}