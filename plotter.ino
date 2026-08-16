#include <Servo.h>

Servo m1, m2, m3, m4;

int buttonPin = 2;
int ledPin = 3;

int penRaiseAngle = 150;

int arrLength = sizeof(xPoints) / sizeof(xPoints[0]);

String input = "";
boolean penRaised;
void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200); //baud rate has to match jSerialComm rate
  
  m1.attach(6);
  m2.attach(7);
  m3.attach(9);
  m4.attach(10);

  m1.write(45);
  m2.write(135);
  m3.write(penRaiseAngle);
  m4.write(penRaiseAngle);
  penRaised = true;

  pinMode(buttonPin, INPUT_PULLUP);
  pinMode(ledPin, OUTPUT);

  digitalWrite(ledPin, LOW);

 
}

void loop() {


  if (Serial.available()) {
    input = Serial.readStringUntil('\n');
    digitalWrite(ledPin, HIGH);

    if (input.startsWith("X")) {
      float x = parseX(input);
      float y = parseY(input);
      
      if (x == 0 && y == 0) penUp();
      else if (x == 1 && y == 1) penDown();
      else moveTo(x, y); 
      

      Serial.println("OK"); //tell Java we're ready
    }
    else if (input == "DONE") {
      Serial.println("FINISHED");
    }
  }

  else { //if no signal from Java run button detection loop
    digitalWrite(ledPin, LOW);
    if (digitalRead(buttonPin) == LOW) {
      while (digitalRead(buttonPin) == LOW) delay(5);

      penRaised = !penRaised;
      if (penRaised) penUp();
      else penDown();
    }
  } 

}

//X## Y##
float parseX(String s) {
  int xIndex = s.indexOf('X');
  int yIndex = s.indexOf('Y');

  if (xIndex != -1 && yIndex != -1) {
    float x = s.substring(xIndex + 1, yIndex).toFloat();
    return x;
  }

  return -1;
}float parseY(String s) {
  int xIndex = s.indexOf('X');
  int yIndex = s.indexOf('Y');

  if (xIndex != -1 && yIndex != -1) {
    
    float y = s.substring(yIndex + 1).toFloat();

    return y;
  }

  return -1;
}

float solveAngle1(float x, float y) {
  float dx = x + 16; //left motor offset from center
  float dy = y;

  float d = sqrtf(dx*dx + dy*dy);

  if (d > 140) return -1; //point is unreachable

  float alpha = degrees(acosf(d/140));

  float theta = degrees(atan2f(dy, dx)) + alpha;

  return theta - 45;
}

float solveAngle2(float x, float y) {
  float dx = x - 16; //right motor offset from center
  float dy = y;

  float d = sqrtf(dx*dx + dy*dy);

  if (d > 140) return -1;

  float alpha = degrees(acosf(d/140));

  float theta = degrees(atan2f(dy, dx)) - alpha;

  return theta + 45;
}

void moveTo(float x, float y) {
  float m1Angle = solveAngle1(x, y);
  float m2Angle = solveAngle2(x, y);

  if (m1Angle == -1) {
    Serial.println("Unreachable point (m1): " + String(x) + ", " + String(y));
    return;
  }
  if (m2Angle == -1) {
    Serial.println("Unreachable point (m2): " + String(x) + ", " + String(y));
    return;
  }

  moveServosSmooth(m1, m2, m1.read(), m2.read(), m1Angle, m2Angle);
}


void moveServosSmooth(
  Servo &servo1, Servo &servo2,
  float start1, float start2,
  float target1, float target2
) {
  float delta1 = target1 - start1;
  float delta2 = target2 - start2;

  // Determine number of steps based on largest movement
  int steps = max(abs(delta1), abs(delta2)) / 0.3; // 0.5° resolution

  if (steps < 1) steps = 1;

  for (int i = 0; i <= steps; i++) {
    float t = (float)i / steps;

    float angle1 = start1 + t * delta1;
    float angle2 = start2 + t * delta2;

    servo1.write(angle1);
    servo2.write(angle2);

    delay(3); //adjust for speed/smoothness
  }
}

void penUp() {
  delay(400);
  m3.write(penRaiseAngle);
  m4.write(penRaiseAngle);
  delay(400);
}
void penDown() {
  delay(400);
  m3.write(180);
  m4.write(180);
  delay(400);
}
