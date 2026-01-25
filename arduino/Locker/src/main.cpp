#include <Keypad.h>

const byte ROWS = 4;
const byte COLS = 4;
#define GreenPin 12
#define redPin 11
#define buttonPin 10

char keys[ROWS][COLS] = {
  {'1','2','3','A'},
  {'4','5','6','B'},
  {'7','8','9','C'},
  {'*','0','#','D'}
};

byte rowPins[ROWS] = {9, 8, 7, 6};
byte colPins[COLS] = {5, 4, 3, 2};

Keypad keypad = Keypad(makeKeymap(keys), rowPins, colPins, ROWS, COLS);
void autenticate();
void setup() {
  Serial.begin(9600);
  Serial.println("Keypad Connection Test");
  pinMode(GreenPin,OUTPUT);
  pinMode(redPin,OUTPUT);
  pinMode(buttonPin,INPUT);
}

void loop() {
  autenticate();
} 
void autenticate(){
  String password = "1234";
  String input = "";
  bool buttonPressed = digitalRead(buttonPin);
  Serial.println("Enter Password: ");
  
  while (!buttonPressed) {
    char key = keypad.getKey();
    if (key and key!= '#') {
      Serial.print("*");
      input += key;
      
    }
    else if (key == '#') {
      if (input == password) {
        Serial.println("\nAccess Granted");
        digitalWrite(GreenPin,HIGH);
        delay(5000);
        digitalWrite(GreenPin,LOW);
        break;
      } 
      else {
        Serial.println("\nAccess Denied");
        digitalWrite(redPin,HIGH);     
        for(;;);

      }
      input = "";
      Serial.println("Enter Password: ");
    }
}}
