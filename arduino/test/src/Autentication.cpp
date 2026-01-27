#include <Arduino.h>
#include <Servo.h>



class digitalPin {
private:
    unsigned int digital_pin;
public:
    digitalPin(int digitalPin) : digital_pin(digitalPin) {
        pinMode(digital_pin, OUTPUT); }

    void on() {
        digitalWrite(digital_pin, HIGH);
    }

    void off() {
        digitalWrite(digital_pin, LOW);
    }
};

// classes for led 
class Buzzer : public digitalPin {
public:
    Buzzer(int buzzPin) : digitalPin(buzzPin) {}
};

class Led : public digitalPin{
    public:
        Led(int ledPin) : digitalPin(ledPin){}
};

//servo

class Door {
private:
    unsigned int door_pin;
    Servo s;

public:
    Door(unsigned int pin) : door_pin(pin) {}

    void init() {
        s.attach(door_pin);
        s.write(0);
    }

    void open() {
        for (int i = 0; i <= 180; i++) {
            s.write(i);
            delay(15);
        }
    }

    void close() {
        for (int i = 180; i >= 0; i--) {
            s.write(i);
            delay(15);
        }
    }
};


// // RFID
// Door Lockerdoor(5);

// void setup() {
//     Lockerdoor.init();
// }

// void loop() {
//     Lockerdoor.open();
//     delay(2000);
//     Lockerdoor.close();
//     delay(2000);
// }
