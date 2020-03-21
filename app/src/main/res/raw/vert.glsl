attribute vec4 vertex;
attribute vec4 normal;
uniform mat4 MVP;
uniform mat4 rotMat;

varying vec4 varyNormal;

void main() {
    varyNormal = rotMat * normal;
    gl_Position = MVP * vertex;
}
