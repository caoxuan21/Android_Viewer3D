// 0; draw mesh
// 1: draw line
uniform int drawMode;


highp vec4 lightDirec = normalize(vec4(1.0, -1.0, 5.0, 1.0));
highp vec4 meshColor = vec4(1.0, 1.0, 1.0, 1.0);

varying highp vec4 varyNormal;
varying highp vec4 lineColor;

void main() {
    highp vec4 diffuse = 0.15 * meshColor;
    highp vec4 phone = dot(lightDirec, normalize(varyNormal)) * meshColor;
    if (drawMode == 0){
        gl_FragColor = diffuse + phone;
    }
    else if (drawMode == 1){
        gl_FragColor = lineColor;
    }
}