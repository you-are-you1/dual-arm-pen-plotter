# dual-arm-pen-plotter
Arduino + 3D printing project

https://github.com/user-attachments/assets/2af5256a-6d67-42be-beb4-859353c3c222

This is a robotic pen plotter controlled by an Arduino Uno and a Java Swing GUI. The arms and pen are controlled by SG90 motors.

## plotter.ino
This program recieves points from the GUI via serial communication, then uses inverse kinematics to convert them to motor angles. It uses a smooth drawing function to move both arms simultaneously and reduce jaggedness in the drawing.

## plotterGUI.java
This is a Java Swing GUI which allows the user to make drawing paths and send them to the Arduino. It converts points on the screen to coordinates in millimetres which the Arduino can use. The points are sent using the jSerialComm library.

## Wiring
Left motor - D6

Right motor - D7

Pen motor - D9

LED - D3 (indicates communication with Java)

Button - D2 (manually raise/lower pen)
