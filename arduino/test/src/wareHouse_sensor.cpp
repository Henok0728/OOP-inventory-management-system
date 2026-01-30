#include <Arduino.h>
#include <DHT.h>

class outTemp{
    private:
        float voltage;
        float tempc;
        int adcValue;
        int pin_m;
    public:
        outTemp(int pin): pin_m(pin){}
        float getOutTemp(){
            int adcValue = analogRead(pin_m);
            float voltage = adcValue * (5.0 / 1023.0);  
            float tempc = voltage * 100.0;  
            return tempc; 
        }
        void sendToJava() {
            Serial.println(getOutTemp());
            }
        
};
class humidity_and_temp {
private:
    float temp;
    float humidity;
    uint8_t pin;
    uint8_t type;
    DHT dht;

public:
    humidity_and_temp(uint8_t dht_pin, uint8_t DHT_TYPE)
        : pin(dht_pin), type(DHT_TYPE), dht(dht_pin, DHT_TYPE) {}

    void init() {
        dht.begin();
    }

    float getTemp() {
        temp = dht.readTemperature();   // Celsius
        if (isnan(temp)) return -1000;  // error code
        return temp;
    }

    float getHumidity() {
        humidity = dht.readHumidity();
        if (isnan(humidity)) return -1; // error code
        return humidity;
    }

    void send_to_java() {
        Serial.print("TEMP:");
        Serial.println(getTemp());

        Serial.print("HUM:");
        Serial.println(getHumidity());
    }
};


#define outpin A0
#define DHTPIN 2
#define DHTTYPE DHT11

humidity_and_temp ht(DHTPIN,DHT11);
outTemp tmp(outpin);
void setup(){
    ht.init();
    Serial.begin(9600);
}
void loop(){
    delay(2000);
    ht.send_to_java();
    tmp.sendToJava();
}
