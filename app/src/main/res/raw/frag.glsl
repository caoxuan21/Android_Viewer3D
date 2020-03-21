highp vec4 lightDirec = normalize(vec4(1.0, -1.0, 5.0, 1.0));
highp vec4 meshColor = vec4(1.0, 1.0, 1.0, 1.0);

varying highp vec4 varyNormal;

void main() {
    highp vec4 diffuse = 0.15 * meshColor;
    highp vec4 phone = dot(lightDirec, normalize(varyNormal)) * meshColor;
    gl_FragColor = diffuse + phone;
}