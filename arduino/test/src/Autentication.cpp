#include <Arduino.h>
#include <Servo.h>
#include <SPI.h>
#include <MFRC522.h>

// WAREHOUSE DOOR SYSTEM DESIGNED BY TEAM ARADA

class digitalPin {
private:
    unsigned int digital_pin;
public:
    digitalPin(int digitalPin) : digital_pin(digitalPin) {}

    void init(){
        pinMode(digital_pin, OUTPUT);
    }

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
// RFID 
class rfid {
  private:
    MFRC522 rf;

  public:
    rfid(byte SS_PIN, byte RST_PIN) : rf(SS_PIN, RST_PIN) {}

    void init() {
      SPI.begin();
      rf.PCD_Init();
    }

    String getUid() {
      if (!rf.PICC_IsNewCardPresent()) return "";
      if (!rf.PICC_ReadCardSerial()) return "";

      String uid = "";
      for (byte i = 0; i < rf.uid.size; i++) {
        if (rf.uid.uidByte[i] < 0x10) uid += "0";
        uid += String(rf.uid.uidByte[i], HEX);
      }
      uid.toUpperCase();

      rf.PICC_HaltA();
      rf.PCD_StopCrypto1();

      return uid;
    }

    bool authenticate(String uid) {
      String allowed[] = {"D5498005", "A0D7BB32"};
      int size = sizeof(allowed) / sizeof(allowed[0]);

      for (int i = 0; i < size; i++) {
        if (allowed[i].equals(uid)) {
          return true;
        }
      }
      return false;
    }

    void sendToJava(String uid) {
      Serial.println("RFID_SCAN:" + uid);
    }
};

#define buzzerPin 3
#define redPin 4
#define doorPin 6
#define SS_PIN 10
#define RST_PIN 9

// initialize all functions
Buzzer buzz(buzzerPin);
Led closedLed(redPin);
Door door(doorPin);
rfid RFID(SS_PIN,RST_PIN);

void setup(){
    buzz.init();
    closedLed.init();
    door.init();
    RFID.init();
    Serial.begin(9600);
    Serial.println("the door is ready");
}
void loop() {
    String uid = RFID.getUid();
    if (uid == "") return;

    Serial.println("UID: " + uid);

    if (RFID.authenticate(uid)) {
        Serial.println("AUTHORIZED");
        RFID.sendToJava(uid);
        buzz.on();
        delay(500);
        buzz.off();

        door.open();
        delay(5000);
        door.close();
      
        delay(2000);
}
    else{
        Serial.println("DENIED");

        for (int i = 0; i < 3; i++) {
            buzz.on();
            delay(300);
            buzz.off();
            delay(300);
        }

        closedLed.on();
        delay(4000);
        closedLed.off();
    }
}