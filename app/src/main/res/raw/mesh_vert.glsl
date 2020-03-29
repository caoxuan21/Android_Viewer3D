attribute vec4 vertex;
attribute vec4 normal;
uniform mat4 MVP;
uniform mat4 rotMat;

varying vec4 lineColor;
varying vec4 varyNormal;

void main() {
    lineColor = vec4(0.3, 0.3, 0.3, 1.0);
    varyNormal = rotMat * normal;
    gl_Position = MVP * vertex;
}
